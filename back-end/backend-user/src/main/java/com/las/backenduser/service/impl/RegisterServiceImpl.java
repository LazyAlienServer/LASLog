package com.las.backenduser.service.impl;

import com.las.backenduser.mapper.UserMapper;
import com.las.backenduser.model.Password;
import com.las.backenduser.model.User;
import com.las.backenduser.model.dto.register.RegisterCompleteDTO;
import com.las.backenduser.service.RegisterService;
import com.las.backenduser.utils.salt.Salt;
import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RegisterServiceImpl implements RegisterService {
    String tokenSalt = "八重神子让我魅力无限，有一次我们班平均分倒数第一，老师气坏了，指着我们破口大骂，你们是我带过的最差的一届！我心想，这么骂下去也不是办法，有什么办法可以控制老师情绪了，有了，我想起了原神中最有魅力的八重神子，于是我模仿着八重的姿态走上了讲台，摸了摸老师的头，怎么啦，小家伙，你看起来不太开心，说完，我舔了舔爪子，学狐狸嗷呜叫了起来，全班同学都震惊到了，这就是八重神子吧？太美了，好妖娆，老师也被我迷住了，表情也变得温和，慈祥，后来，这件事被同学们发到网上爆火了起来，各大导演纷纷找到我，让我去演姐己，说我有狐狸那种邪魅的劲，但我都拒绝了，因为八重神子和妲己不一样，一天下午，我打开了手机，公司老总给我发了这样一条信息，你要是姐己，我愿意当你的纣王，你要是八重，我愿意当你的雷电将军！我感动极了，马上找到了公司老总，嗷呜，我模仿着狐狸的模样，老总看到后心都化了，把我拥入怀里，你是我见过，最美的八重神子…";
    String sign = "signature";
    String expireMsa = "expireMs";
    private final RestTemplate restTemplate = new RestTemplate();
    private final UserMapper userMapper;
    private final StringRedisTemplate stringRedisTemplate;

    public static final String TOKEN_BLACKLIST_PREFIX = "register:used_token:";

    @Override
    public String generateToken(String qq, int direction, long expireMs){
        String base64Qq = Base64.getUrlEncoder().withoutPadding().encodeToString(qq.getBytes(StandardCharsets.UTF_8));
        String base64Expire = Base64.getUrlEncoder().withoutPadding().encodeToString(String.valueOf(expireMs).getBytes(StandardCharsets.UTF_8));
        String messageBody = base64Qq + "-" + direction + "-" + base64Expire;
        String signature = DigestUtils.sha256Hex((messageBody + tokenSalt).getBytes(StandardCharsets.UTF_8));
        return messageBody + "." + signature;
    }

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

        String expectedSignature = DigestUtils.sha256Hex((messageBody + tokenSalt).getBytes(StandardCharsets.UTF_8));
        if (!expectedSignature.equals(signature)) {
            throw new IllegalArgumentException("无效的激活链接");
        }

        String[] bodyParts = messageBody.split("-");
        if (bodyParts.length != 3) {
            throw new IllegalArgumentException("Token消息体解析失败");
        }

        String qq;
        int direction;
        long expireMs;
        try {
            qq = new String(Base64.getUrlDecoder().decode(bodyParts[0]), StandardCharsets.UTF_8);
            direction = Integer.parseInt(bodyParts[1]);
            expireMs = Long.parseLong(new String(Base64.getUrlDecoder().decode(bodyParts[2]), StandardCharsets.UTF_8));
        } catch (Exception e) {
            // 只有真正的解析失败才抛这个
            throw new IllegalArgumentException("Token Base64解码异常");
        }

        // 过期校验必须放在 try-catch 的外面！
        if (System.currentTimeMillis() > expireMs) {
            throw new IllegalArgumentException("该激活链接已过期");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("qq", qq);
        result.put("direction", direction);
        result.put(expireMsa , expireMs);
        result.put(sign, signature);
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
        // 1. 基础校验
        Map<String, Object> userInfo = this.verifyAndDecodeToken(token);
        String signature = (String) userInfo.get(sign);

        // 2. Redis 黑名单检查
        String redisKey = TOKEN_BLACKLIST_PREFIX + signature;
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(redisKey))) {
            throw new IllegalArgumentException("该激活链接已被使用");
        }

        // 3. 数据清洗 (脱敏)
        HashMap<String, Object> returnData = new HashMap<>(userInfo);
        returnData.remove(expireMsa);
        returnData.remove(sign);

        return returnData;
    }

    @Override
    @Transactional(rollbackFor = Exception.class) // 保证数据库插入异常时事务回滚
    public void completeRegister(RegisterCompleteDTO dto) throws IOException {
        if (dto.getToken() == null || dto.getMinecraftId() == null || dto.getPassword() == null || dto.getUsername() == null) {
            throw new IllegalArgumentException("注册信息不完整");
        }

        // 1. 基础校验
        Map<String, Object> userInfo = this.verifyAndDecodeToken(dto.getToken());
        String qq = (String) userInfo.get("qq");
        long expireMs = (Long) userInfo.get("expireMs");
        String signature = (String) userInfo.get("sign");

        // 2. Redis 拦截
        String redisKey = TOKEN_BLACKLIST_PREFIX + signature;
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(redisKey))) {
            throw new IllegalArgumentException("该激活链接已被使用");
        }

        Long existCount = userMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>()
                        .eq(User::getUsername, dto.getUsername())
        );
        if (existCount != null && existCount > 0) {
            throw new IllegalArgumentException("该用户名已被注册，请更换一个");
        }

        // 4. Mojang API 校验
        String uuid = this.checkMinecraftId(dto.getMinecraftId());
        if (uuid == null) {
            throw new IllegalArgumentException("Minecraft ID 不存在或非正版");
        }

        // 5. 密码加盐处理
        Password securePassword = Salt.salt(dto.getPassword());

        // 6. 保存到 PostgreSQL
        User user = new User();
        user.setQq(Long.valueOf(qq));
        user.setUsername(dto.getUsername());
        user.setMainMinecraftUuid(dto.getMinecraftId());
        user.setIdMinecraft(Collections.singletonList(dto.getMinecraftId()));
        user.setUuidMinecraft(Collections.singletonList(uuid));
        user.setUuid(String.valueOf(UUID.randomUUID()));
        user.setSalt(securePassword.getSalt());
        user.setPassword(securePassword.getCipherText());
        user.setRegisterdate(System.currentTimeMillis()+ Duration.ofHours(8).toMillis());
        user.setPermission(Collections.singletonList("TEST"));
        user.setWhitelist(Collections.singletonList("TEST"));
        user.setStatus(1);
        user.setMainMinecraftUuid(uuid);
        userMapper.insert(user);

        // 7. Token 存入 Redis 黑名单
        long remainingMs = expireMs - System.currentTimeMillis();
        if (remainingMs > 0) {
            stringRedisTemplate.opsForValue().set(redisKey, "1", remainingMs, TimeUnit.MILLISECONDS);
        }
    }
}