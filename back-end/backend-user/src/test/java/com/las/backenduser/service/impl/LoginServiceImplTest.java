package com.las.backenduser.service.impl;

import com.las.backenduser.mapper.UserMapper;
import com.las.backenduser.model.User;
import com.las.backenduser.service.db.redis.impl.RedisToolsImpl;
import com.las.backenduser.utils.jwt.JwtUtils;
import com.las.backenduser.utils.result.Result;
import com.las.backenduser.utils.result.ResultEnum;
import com.las.backenduser.utils.salt.Salt;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.io.Serializable;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoginServiceImplTest {

    @Mock private RedisToolsImpl redisTools;
    @Mock private UserMapper userMapper;
    @Mock private JwtUtils jwtUtils;
    @Mock private StringRedisTemplate stringRedisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private LoginServiceImpl loginService;

    @Test
    @DisplayName("登录 - 成功且有旧Token需要清理")
    void login_Success_WithOldToken() {
        String userName = "sunyinuo";
        String passwd = "correct_password";
        String clientId = "pc";
        String uuid = "uuid-123";
        String hashedPw = "hashed_pw";

        User mockUser = new User();
        mockUser.setUsername(userName);
        mockUser.setPassword(hashedPw);
        mockUser.setSalt("salt");
        mockUser.setUuid(uuid);

        when(userMapper.selectOne(any())).thenReturn(mockUser);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("auth:rt:user:" + uuid + ":" + clientId)).thenReturn("\"old_rt\""); // 模拟存在旧RT

        when(jwtUtils.createRefreshToken()).thenReturn("new_rt");
        when(jwtUtils.createAccessToken(uuid, userName)).thenReturn("new_at");

        try (MockedStatic<Salt> saltMockedStatic = mockStatic(Salt.class)) {
            saltMockedStatic.when(() -> Salt.salt(eq(passwd), anyString())).thenReturn(hashedPw);

            Result<String> result = loginService.login(userName, passwd, clientId);

            assertEquals(ResultEnum.SUCCESS.getCode(), result.getCode());
            // 验证旧Token被清理
            verify(stringRedisTemplate).delete("auth:rt:token:old_rt:" + clientId);
            verify(stringRedisTemplate).delete("auth:rt:user:" + uuid + ":" + clientId);
        }
    }

    @Test
    @DisplayName("踢出用户 - 成功清理所有设备")
    void kickOut_Success() {
        String uuid = "uuid-123";
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(stringRedisTemplate.keys("auth:rt:user:" + uuid + ":*"))
                .thenReturn(new HashSet<>(Collections.singletonList("auth:rt:user:uuid-123:pc")));
        when(valueOperations.get("auth:rt:user:uuid-123:pc")).thenReturn("old_rt");

        loginService.kickOut(uuid);

        verify(valueOperations).set(eq("login:kickout:" + uuid), anyString(), eq(14L), eq(TimeUnit.DAYS));
        verify(stringRedisTemplate).delete("auth:rt:token:old_rt:pc");
        verify(stringRedisTemplate).delete("auth:rt:user:uuid-123:pc");
    }

    @Test
    @DisplayName("Token登录 - 成功")
    void loginByToken_Success() {
        String token = "valid_at";
        String uuid = "uuid-123";

        // 做法二：直接 Mock Claims 接口，不依赖 jjwt 的底层实现类
        Claims mockClaims = mock(Claims.class);
        // 模拟返回一个未来的时间 (说明没有过期)
        when(mockClaims.getIssuedAt()).thenReturn(new Date(System.currentTimeMillis() + 10000));

        when(jwtUtils.parseToken(token)).thenReturn(mockClaims);
        when(jwtUtils.getUserUUIDFromToken(token)).thenReturn(uuid);

        User user = new User();
        user.setUuid(uuid);
        when(userMapper.selectOne(any())).thenReturn(user);

        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("login:kickout:" + uuid)).thenReturn(null); // 未被踢出

        Result<Serializable> result = loginService.loginByToken(token, "pc");
        assertEquals(ResultEnum.SUCCESS.getCode(), result.getCode());
    }

    @Test
    @DisplayName("Token登录 - 已被踢出")
    void loginByToken_KickedOut() {
        String token = "valid_at";
        String uuid = "uuid-123";

        Claims mockClaims = mock(Claims.class);
        // 模拟签发时间在踢出之前 (过去的时间)
        when(mockClaims.getIssuedAt()).thenReturn(new Date(System.currentTimeMillis() - 10000));

        when(jwtUtils.parseToken(token)).thenReturn(mockClaims);
        when(jwtUtils.getUserUUIDFromToken(token)).thenReturn(uuid);

        User user = new User();
        user.setUuid(uuid);
        when(userMapper.selectOne(any())).thenReturn(user);

        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("login:kickout:" + uuid)).thenReturn(String.valueOf(System.currentTimeMillis())); // 模拟被踢出记录

        Result<Serializable> result = loginService.loginByToken(token, "pc");
        assertEquals(ResultEnum.UNAUTHORIZED.getCode(), result.getCode());
        assertEquals("KICKED", result.getMsg());
    }

    @Test
    @DisplayName("刷新Token - 成功")
    void refreshToken_Success() {
        String rt = "valid_rt";
        String clientId = "pc";
        String uuid = "uuid-123";

        when(redisTools.getByKey("auth:rt:token:" + rt + ":" + clientId)).thenReturn(rt);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("auth:rt:token:" + rt + ":" + clientId)).thenReturn("\"" + uuid + "\"");

        User mockUser = new User();
        mockUser.setUsername("test_user");
        when(userMapper.selectOne(any())).thenReturn(mockUser);
        when(jwtUtils.createAccessToken(uuid, "test_user")).thenReturn("new_at");

        Result<Serializable> result = loginService.refreshToken(rt, clientId);

        assertEquals(ResultEnum.SUCCESS.getCode(), result.getCode());
        assertEquals("new_at", result.getData());
    }
}