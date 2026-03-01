package com.las.backenduser.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.las.backenduser.mapper.UserMapper;
import com.las.backenduser.model.Password;
import com.las.backenduser.model.User;
import com.las.backenduser.model.dto.register.RegisterCompleteDTO;
import com.las.backenduser.service.RegisterService;
import com.las.backenduser.utils.salt.Salt;
import lombok.RequiredArgsConstructor;
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
public class RegisterServiceImpl implements RegisterService {

    @Value("${jwt.token-salt}")
    private String tokenSalt;

    private final RestTemplate restTemplate;
    private final UserMapper userMapper;
    private final StringRedisTemplate stringRedisTemplate;

    public static final String TOKEN_BLACKLIST_PREFIX = "register:used_token:";

    private static final String KEY_QQ = "qq";
    private static final String KEY_DIRECTION = "direction";
    private static final String KEY_EXPIRE_MS = "expireMs";
    private static final String KEY_SIGNATURE = "signature";

    @Override
    public String generateToken(String qq, int direction, long expireMs) {
        String base64Qq = Base64.getUrlEncoder().withoutPadding().encodeToString(qq.getBytes(StandardCharsets.UTF_8));
        String base64Expire = Base64.getUrlEncoder().withoutPadding().encodeToString(String.valueOf(expireMs).getBytes(StandardCharsets.UTF_8));

        String messageBody = base64Qq + "-" + direction + "-" + base64Expire;
        String signature = DigestUtils.sha256Hex((messageBody + tokenSalt).getBytes(StandardCharsets.UTF_8));

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
            // 文案与测试用例精确对齐
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
        user.setIdMinecraft(Collections.singletonList(dto.getMinecraftId()));
        user.setUuidMinecraft(Collections.singletonList(uuid));
        user.setUuid(UUID.randomUUID().toString());
        user.setSalt(securePassword.getSalt());
        user.setPassword(securePassword.getCipherText());

        // 使用标准的绝对时间戳
        user.setRegisterdate(System.currentTimeMillis());
        user.setPermission(Collections.singletonList("TEST"));
        user.setWhitelist(Collections.singletonList("TEST"));
        user.setStatus(1);

        // 插入数据库
        userMapper.insert(user);

        // 注册成功后，计算 Token 剩余有效期，并将其加入 Redis 黑名单以使其作废
        long remainingMs = expireMs - System.currentTimeMillis();
        if (remainingMs > 0) {
            String redisKey = TOKEN_BLACKLIST_PREFIX + signature;
            stringRedisTemplate.opsForValue().set(redisKey, "1", remainingMs, TimeUnit.MILLISECONDS);
        }
    }
}