package com.las.backenduser.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.las.backenduser.mapper.UserMapper;
import com.las.backenduser.model.Password;
import com.las.backenduser.model.User;
import com.las.backenduser.model.dto.register.RecentRegistrationVO;
import com.las.backenduser.model.dto.register.RegisterCompleteDTO;
import com.las.backenduser.service.RegisterService;
import com.las.backenduser.utils.salt.Salt;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class RegisterServiceImpl implements RegisterService {

    @Value("${secret.jwt.token-salt}")
    private String tokenSalt;

    private final RestTemplate restTemplate;
    private final UserMapper userMapper;
    private final StringRedisTemplate stringRedisTemplate;

    public static final String TOKEN_BLACKLIST_PREFIX = "register:used_token:";
    public static final String ENTRY_PREFIX = "register:entry:";

    // 字段常量
    private static final String KEY_QQ = "qq";
    private static final String KEY_DIRECTION = "direction";
    private static final String KEY_EXPIRE_MS = "expireMs";
    private static final String KEY_SIGNATURE = "signature";
    private static final String KEY_STATUS = "status";
    private static final String KEY_MINECRAFT_ID = "minecraftId";
    private static final String KEY_CREATE_TIME = "createTime";
    private static final String KEY_EXPIRE_TIME = "expireTime";

    // 状态常量
    private static final String STATUS_WAITING = "WAITING";
    private static final String STATUS_ACTIVATED = "ACTIVATED";
    private static final String STATUS_INVALIDATED = "INVALIDATED";

    @Override
    public String generateToken(String qq, int direction, long expireMs) {
        String base64Qq = Base64.getUrlEncoder().withoutPadding().encodeToString(qq.getBytes(StandardCharsets.UTF_8));
        String base64Expire = Base64.getUrlEncoder().withoutPadding().encodeToString(String.valueOf(expireMs).getBytes(StandardCharsets.UTF_8));

        String messageBody = base64Qq + "-" + direction + "-" + base64Expire;
        String signature = DigestUtils.sha256Hex((messageBody + tokenSalt).getBytes(StandardCharsets.UTF_8));

        // 在 Redis 中记录此条目，用于"最近注册"面板展示
        // TTL = 48h（24h链接有效期 + 24h展示期）
        String entryKey = ENTRY_PREFIX + signature;
        Map<String, String> entryData = new HashMap<>();
        entryData.put(KEY_QQ, qq);
        entryData.put(KEY_DIRECTION, String.valueOf(direction));
        entryData.put(KEY_STATUS, STATUS_WAITING);
        entryData.put(KEY_CREATE_TIME, String.valueOf(System.currentTimeMillis()));
        entryData.put(KEY_EXPIRE_TIME, String.valueOf(expireMs));
        stringRedisTemplate.opsForHash().putAll(entryKey, entryData);
        stringRedisTemplate.expire(entryKey, 48, TimeUnit.HOURS);

        return messageBody + "." + signature;
    }

    /**
     * 核心校验方法：集成了格式、防篡改、过期时间以及 Redis 黑名单（防重放）四大校验。
     * 顺序和文案已严格按照单元测试的要求对齐。
     */
    @Override
    public Map<String, Object> verifyAndDecodeToken(String token) {
        if (token == null || !token.contains(".")) {
            throw new IllegalArgumentException("无效的激活链接");
        }

        String[] parts = token.split("\\.");
        if (parts.length != 2) {
            throw new IllegalArgumentException("无效的激活链接");
        }

        String messageBody = parts[0];
        String signature = parts[1];

        // 1. 防篡改校验（优先级最高，防止被篡改的数据绕过后续验证）
        String expectedSignature = DigestUtils.sha256Hex((messageBody + tokenSalt).getBytes(StandardCharsets.UTF_8));
        if (!expectedSignature.equals(signature)) {
            // 文案与测试用例精确对齐
            throw new IllegalArgumentException("Token签名校验失败，可能已被篡改");
        }

        // 2. 消息体结构校验
        String[] bodyParts = messageBody.split("-");
        if (bodyParts.length != 3) {
            throw new IllegalArgumentException("Token消息体解析失败");
        }

        String qq;
        int direction;
        long expireMs;
        // 3. 解析字段及 Base64 解码校验
        try {
            qq = new String(Base64.getUrlDecoder().decode(bodyParts[0]), StandardCharsets.UTF_8);
            direction = Integer.parseInt(bodyParts[1]);
            expireMs = Long.parseLong(new String(Base64.getUrlDecoder().decode(bodyParts[2]), StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalArgumentException("Token Base64解码异常");
        }

        // 4. 过期时间校验（放在最后判断，确保格式肯定是对的）
        if (System.currentTimeMillis() > expireMs) {
            throw new IllegalArgumentException("该激活链接已过期");
        }

        // 5. Redis 黑名单校验（防重放攻击）
        String redisKey = TOKEN_BLACKLIST_PREFIX + signature;
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(redisKey))) {
            throw new IllegalArgumentException("该激活链接已被使用");
        }

        Map<String, Object> result = new HashMap<>();
        result.put(KEY_QQ, qq);
        result.put(KEY_DIRECTION, direction);
        result.put(KEY_EXPIRE_MS, expireMs);
        result.put(KEY_SIGNATURE, signature);
        return result;
    }

    @Override
    public String checkMinecraftId(String username) throws IOException {
        String url = "https://api.mojang.com/users/profiles/minecraft/" + username;
        try {
            var response = restTemplate.getForObject(url, Map.class);
            if (response != null && response.containsKey("id")) {
                return response.get("id").toString();
            }
            return null;
        } catch (HttpClientErrorException.NotFound e) {
            return null;
        } catch (Exception e) {
            throw new IOException("请求Mojang API失败", e);
        }
    }

    @Override
    public HashMap<String, Object> activateToken(String token) {
        // 调用底层的校验方法（已经包含了 Redis 黑名单检查）
        Map<String, Object> userInfo = this.verifyAndDecodeToken(token);

        // 数据脱敏：移除不应返回给前端的安全字段
        HashMap<String, Object> returnData = new HashMap<>(userInfo);
        returnData.remove(KEY_EXPIRE_MS);
        returnData.remove(KEY_SIGNATURE);

        return returnData;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeRegister(RegisterCompleteDTO dto) throws IOException {
        if (dto.getToken() == null || dto.getMinecraftId() == null || dto.getPassword() == null || dto.getUsername() == null) {
            throw new IllegalArgumentException("注册信息不完整");
        }

        // 调用底层的校验方法（已经包含了 Redis 黑名单检查）
        Map<String, Object> userInfo = this.verifyAndDecodeToken(dto.getToken());
        String qq = (String) userInfo.get(KEY_QQ);
        long expireMs = (Long) userInfo.get(KEY_EXPIRE_MS);
        String signature = (String) userInfo.get(KEY_SIGNATURE);

        // 校验用户名唯一性
        Long existCount = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername())
        );
        if (existCount != null && existCount > 0) {
            throw new IllegalArgumentException("该用户名已被注册，请更换一个");
        }

        // 校验 Minecraft 正版账号有效性
        String uuid = this.checkMinecraftId(dto.getMinecraftId());
        if (uuid == null) {
            throw new IllegalArgumentException("Minecraft ID 不存在或非正版");
        }

        // 密码加盐处理
        Password securePassword = Salt.salt(dto.getPassword());

        // 构建用户实体
        User user = new User();
        user.setQq(Long.valueOf(qq));
        user.setUsername(dto.getUsername());
        user.setMainMinecraftUuid(uuid);
        user.setMinecraftIds(Collections.singletonList(dto.getMinecraftId()));
        user.setMinecraftUuids(Collections.singletonList(uuid));
        user.setUuid(UUID.randomUUID().toString());
        user.setSalt(securePassword.getSalt());
        user.setPassword(securePassword.getCipherText());

        // 使用标准的绝对时间戳
        user.setRegisterDate(System.currentTimeMillis());
        user.setPermission(Collections.singletonList("TEST"));
        user.setWhitelist(Collections.singletonList("TEST"));
        user.setStatus(User.STATUS_ACTIVE);

        // 插入数据库
        userMapper.insert(user);

        // 注册成功后，计算 Token 剩余有效期，并将其加入 Redis 黑名单以使其作废
        long remainingMs = expireMs - System.currentTimeMillis();
        if (remainingMs > 0) {
            String redisKey = TOKEN_BLACKLIST_PREFIX + signature;
            stringRedisTemplate.opsForValue().set(redisKey, "1", remainingMs, TimeUnit.MILLISECONDS);
        }

        // 更新 Redis 条目状态为已激活，记录 MinecraftID，TTL 重置为 24h
        String entryKey = ENTRY_PREFIX + signature;
        boolean entryExists = Boolean.TRUE.equals(stringRedisTemplate.hasKey(entryKey));
        log.info("注册完成 - signature={}, entryExists={}", signature, entryExists);
        if (entryExists) {
            stringRedisTemplate.opsForHash().put(entryKey, KEY_STATUS, STATUS_ACTIVATED);
            stringRedisTemplate.opsForHash().put(entryKey, KEY_MINECRAFT_ID, dto.getMinecraftId());
            stringRedisTemplate.expire(entryKey, 24, TimeUnit.HOURS);
        } else {
            // 兼容旧token（生成时未写入entry），补建一条ACTIVATED记录
            Map<String, String> entryData = new HashMap<>();
            entryData.put(KEY_QQ, qq);
            entryData.put(KEY_DIRECTION, String.valueOf(userInfo.get(KEY_DIRECTION)));
            entryData.put(KEY_STATUS, STATUS_ACTIVATED);
            entryData.put(KEY_MINECRAFT_ID, dto.getMinecraftId());
            entryData.put(KEY_CREATE_TIME, String.valueOf(System.currentTimeMillis()));
            entryData.put(KEY_EXPIRE_TIME, String.valueOf(expireMs));
            stringRedisTemplate.opsForHash().putAll(entryKey, entryData);
            stringRedisTemplate.expire(entryKey, 24, TimeUnit.HOURS);
            log.info("补建Redis条目并标记ACTIVATED, qq={}", qq);
        }
    }


    /**
     * 抽取出的私有方法：处理单条最近注册记录的封装及状态修正
     * 降低主方法的认知复杂度 (Cognitive Complexity)
     */
    @Override
    public List<RecentRegistrationVO> getRecentRegistrations() {
        Set<String> keys = stringRedisTemplate.keys(ENTRY_PREFIX + "*");
        if (keys == null || keys.isEmpty()) {
            return Collections.emptyList();
        }

        List<RecentRegistrationVO> list = new ArrayList<>();
        long now = System.currentTimeMillis();

        for (String key : keys) {
            RecentRegistrationVO vo = processSingleRegistration(key, now);
            if (vo != null) {
                list.add(vo);
            }
        }

        // 按创建时间倒序
        list.sort((a, b) -> Long.compare(b.getCreateTime(), a.getCreateTime()));
        return list;
    }

    /**
     * 解析单条注册记录并构建 VO
     */
    private RecentRegistrationVO processSingleRegistration(String key, long now) {
        Map<Object, Object> data = stringRedisTemplate.opsForHash().entries(key);
        if (data.isEmpty()) {
            return null;
        }

        RecentRegistrationVO vo = new RecentRegistrationVO();
        vo.setSignature(key.substring(ENTRY_PREFIX.length()));
        vo.setQq((String) data.get(KEY_QQ));
        vo.setDirection(Integer.parseInt((String) data.getOrDefault(KEY_DIRECTION, "0")));
        vo.setCreateTime(Long.parseLong((String) data.getOrDefault(KEY_CREATE_TIME, "0")));

        long expireTime = Long.parseLong((String) data.getOrDefault(KEY_EXPIRE_TIME, "0"));
        vo.setExpireTime(expireTime);

        String status = (String) data.getOrDefault(KEY_STATUS, STATUS_WAITING);
        String minecraftId = (String) data.get(KEY_MINECRAFT_ID);

        // 1. 如果状态仍为 WAITING 但已过期，自动标记为 INVALIDATED 并重置 TTL 24h
        if (STATUS_WAITING.equals(status) && now > expireTime) {
            status = STATUS_INVALIDATED;
            stringRedisTemplate.opsForHash().put(key, KEY_STATUS, STATUS_INVALIDATED);
            stringRedisTemplate.expire(key, 24, TimeUnit.HOURS);
        }

        vo.setStatus(status);
        vo.setMinecraftId(minecraftId);

        // 2. 数据修复: 如果状态不是 ACTIVATED，尝试通过数据库检查是否已注册并修正
        fixLegacyStatusIfNeeded(key, vo);

        return vo;
    }

    /**
     * 进一步抽离的复杂逻辑：旧代码兼容导致 Redis 未更新状态的自动修复
     */
    private void fixLegacyStatusIfNeeded(String key, RecentRegistrationVO vo) {
        if (STATUS_ACTIVATED.equals(vo.getStatus()) || vo.getQq() == null) {
            return; // 已经是激活状态或无 QQ 号则无需修复，提前 return 降低嵌套
        }

        try {
            Long qqNum = Long.valueOf(vo.getQq());
            User dbUser = userMapper.selectOne(
                    new LambdaQueryWrapper<User>().eq(User::getQq, qqNum).last("LIMIT 1"));

            if (dbUser != null) {
                vo.setStatus(STATUS_ACTIVATED);
                // 简化三元运算符
                if (dbUser.getMinecraftIds() != null && !dbUser.getMinecraftIds().isEmpty()) {
                    vo.setMinecraftId(dbUser.getMinecraftIds().get(0));
                }

                // 同步修复 Redis 数据
                stringRedisTemplate.opsForHash().put(key, KEY_STATUS, STATUS_ACTIVATED);
                if (vo.getMinecraftId() != null) {
                    stringRedisTemplate.opsForHash().put(key, KEY_MINECRAFT_ID, vo.getMinecraftId());
                }
                stringRedisTemplate.expire(key, 24, TimeUnit.HOURS);
            }
        } catch (NumberFormatException ignored) {
            // qq 不是有效数字，跳过
        }
    }

    @Override
    public void invalidateLink(String signature) {
        String entryKey = ENTRY_PREFIX + signature;
        if (Boolean.FALSE.equals(stringRedisTemplate.hasKey(entryKey))) {
            throw new IllegalArgumentException("未找到该激活链接记录");
        }

        String currentStatus = (String) stringRedisTemplate.opsForHash().get(entryKey, KEY_STATUS);
        if (!STATUS_WAITING.equals(currentStatus)) {
            throw new IllegalArgumentException("该链接状态不可失效");
        }

        // 加入黑名单使 token 不可用
        String expireStr = (String) stringRedisTemplate.opsForHash().get(entryKey, KEY_EXPIRE_TIME);
        long expireMs = expireStr != null ? Long.parseLong(expireStr) : 0;
        long remainingMs = expireMs - System.currentTimeMillis();
        if (remainingMs > 0) {
            String blacklistKey = TOKEN_BLACKLIST_PREFIX + signature;
            stringRedisTemplate.opsForValue().set(blacklistKey, "1", remainingMs, TimeUnit.MILLISECONDS);
        }

        // 更新条目状态，TTL 重置为 24h
        stringRedisTemplate.opsForHash().put(entryKey, KEY_STATUS, STATUS_INVALIDATED);
        stringRedisTemplate.expire(entryKey, 24, TimeUnit.HOURS);
    }
}