package com.las.backenduser.service.impl;

import com.las.backenduser.mapper.UserMapper;
import com.las.backenduser.model.User;
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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
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
@MockitoSettings(strictness = Strictness.LENIENT)
class LoginServiceImplTest {

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
        when(valueOperations.get(anyString())).thenReturn("old_rt");

        when(jwtUtils.createRefreshToken()).thenReturn("new_rt");
        when(jwtUtils.createAccessToken(uuid, userName)).thenReturn("new_at");

        try (MockedStatic<Salt> saltMockedStatic = mockStatic(Salt.class)) {
            saltMockedStatic.when(() -> Salt.salt(eq(passwd), anyString())).thenReturn(hashedPw);

            Result<String> result = loginService.login(userName, passwd, clientId);

            assertEquals(ResultEnum.SUCCESS.getCode(), result.getCode());
            verify(stringRedisTemplate).delete("auth:rt:token:old_rt:" + clientId);
            verify(stringRedisTemplate).delete("auth:rt:user:" + uuid + ":" + clientId);
        }
    }

    @Test
    @DisplayName("踢出用户 - 成功清理所有设备")
    void kickOut_Success() {
        String uuid = "uuid-123";
        when(userMapper.selectCount(any())).thenReturn(1L);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(stringRedisTemplate.keys("auth:rt:user:" + uuid + ":*"))
                .thenReturn(new HashSet<>(Collections.singletonList("auth:rt:user:uuid-123:pc")));
        when(valueOperations.get("auth:rt:user:uuid-123:pc")).thenReturn("old_rt");

        loginService.kickOutByUuid(uuid);

        verify(valueOperations).set(eq("login:kickout:" + uuid), anyString(), eq(16L), eq(TimeUnit.MINUTES));
        verify(stringRedisTemplate).delete("auth:rt:token:old_rt:pc");
        verify(stringRedisTemplate).delete("auth:rt:user:uuid-123:pc");
    }

    @Test
    @DisplayName("isKickedOut - 应该返回true当Redis中有且时间匹配")
    void isKickedOut_ShouldReturnTrue_WhenKicked() {
        String uuid = "uuid-123";
        Date now = new Date();
        long tokenTime = now.getTime() - 5000;
        Date tokenDate = new Date(tokenTime);

        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("login:kickout:" + uuid)).thenReturn(String.valueOf(now.getTime()));

        boolean result = loginService.isKickedOut(uuid, tokenDate);
        assertTrue(result);
    }

    @Test
    @DisplayName("isKickedOut - 应该返回false当Redis中无记录")
    void isKickedOut_ShouldReturnFalse_WhenNoRecord() {
        String uuid = "uuid-123";
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("login:kickout:" + uuid)).thenReturn(null);

        boolean result = loginService.isKickedOut(uuid, new Date());
        assertFalse(result);
    }

    @Test
    @DisplayName("Token登录 - 成功")
    void loginByToken_Success() {
        String validToken = "valid_token";
        String clientId = "pc";
        String uuid = "uuid-123";

        io.jsonwebtoken.Claims mockClaims = mock(io.jsonwebtoken.Claims.class);
        when(mockClaims.getSubject()).thenReturn(uuid);
        when(mockClaims.getIssuedAt()).thenReturn(new java.util.Date());
        when(jwtUtils.parseToken(anyString())).thenReturn(mockClaims);

        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null); // Not kicked out
        when(stringRedisTemplate.hasKey(anyString())).thenReturn(true); // Session exists

        Result<java.io.Serializable> result = loginService.loginByToken(validToken, clientId);

        assertEquals(200, result.getCode());
        assertEquals("登录有效", result.getMsg());
    }

    @Test
    @DisplayName("Token登录 - 已被踢出")
    void loginByToken_KickedOut() {
        String token = "valid_at";
        String uuid = "uuid-123";

        Claims mockClaims = mock(Claims.class);
        when(mockClaims.getIssuedAt()).thenReturn(new Date(System.currentTimeMillis() - 10000));
        when(mockClaims.getSubject()).thenReturn(uuid);

        when(jwtUtils.parseToken(token)).thenReturn(mockClaims);

        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("login:kickout:" + uuid)).thenReturn(String.valueOf(System.currentTimeMillis()));

        Result<Serializable> result = loginService.loginByToken(token, "pc");
        assertEquals(ResultEnum.UNAUTHORIZED.getCode(), result.getCode());
        assertEquals("凭证已失效，请重新登录", result.getMsg());
    }

    @Test
    @DisplayName("刷新Token - 成功")
    void refreshToken_Success() {
        String rt = "valid_rt";
        String clientId = "pc";
        String uuid = "uuid-123";

        // LoginServiceImpl uses stringRedisTemplate directly for refreshToken check
        // redisKey = REDIS_RT_TOKEN_PREFIX + refreshToken + ":" + clientId
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("auth:rt:token:" + rt + ":" + clientId)).thenReturn(uuid);

        User mockUser = new User();
        mockUser.setUsername("test_user");
        when(userMapper.selectOne(any())).thenReturn(mockUser);
        when(jwtUtils.createAccessToken(uuid, "test_user")).thenReturn("new_at");

        Result<Serializable> result = loginService.refreshToken(rt, clientId);

        assertEquals(ResultEnum.SUCCESS.getCode(), result.getCode());
        assertEquals("new_at", result.getData());
    }

    @Test
    void kickOut_UserNotFound() {
        when(userMapper.selectCount(any())).thenReturn(0L);

        Result<Serializable> result = loginService.kickOutByUuid("invalid-uuid");

        assertEquals(404, result.getCode());
        assertEquals("未找到用户", result.getMsg());
        verify(stringRedisTemplate, never()).keys(anyString());
    }

    @Test
    void kickOut_Success_AlreadyOffline() {
        when(userMapper.selectCount(any())).thenReturn(1L);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(stringRedisTemplate.keys(anyString())).thenReturn(new java.util.HashSet<>());

        Result<Serializable> result = loginService.kickOutByUuid("uuid-123");

        assertEquals(200, result.getCode());
        assertEquals("未找到用户的登录状态或已经踢出", result.getMsg());
    }

    @Test
    void logout_Success() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("auth:rt:user:uuid-123:pc")).thenReturn("old_rt");

        Result<Serializable> result = loginService.logout("uuid-123", "pc");

        assertEquals(200, result.getCode());
        assertEquals("登出成功", result.getMsg());
        verify(stringRedisTemplate).delete("auth:rt:token:old_rt:pc");
        verify(stringRedisTemplate).delete("auth:rt:user:uuid-123:pc");
    }

    @Test
    void logout_AlreadyOffline() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);

        Result<Serializable> result = loginService.logout("uuid-123", "pc");

        assertEquals(403, result.getCode());
        assertEquals("未找到登录状态或已下线", result.getMsg());
    }
}
