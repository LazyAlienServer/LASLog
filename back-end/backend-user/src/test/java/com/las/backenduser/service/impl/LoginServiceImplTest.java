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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.io.Serializable;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
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
        when(valueOperations.get(anyString())).thenReturn("old_rt");// 模拟存在旧RT

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
    void loginByToken_Success() {
        String validToken = "valid_token";
        String clientId = "pc";
        String uuid = "uuid-123";

        // 1. 模拟 JWT 解析成功
        io.jsonwebtoken.Claims mockClaims = mock(io.jsonwebtoken.Claims.class);
        when(mockClaims.getSubject()).thenReturn(uuid);
        when(mockClaims.getIssuedAt()).thenReturn(new java.util.Date());
        when(jwtUtils.parseToken(anyString())).thenReturn(mockClaims);

        // 2. 防止 kickOut 逻辑调用 opsForValue() 导致空指针！
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        // 模拟没有被踢出（查不到踢出标记）
        when(valueOperations.get(anyString())).thenReturn(null);

        // 3. 模拟 Redis 的 hasKey，证明设备在线
        when(stringRedisTemplate.hasKey(anyString())).thenReturn(true);

        // 4. 执行测试
        Result<java.io.Serializable> result = loginService.loginByToken(validToken, clientId);

        // 5. 假如这里还报错，你可以临时去 LoginServiceImpl 的 catch 块里加一句 e.printStackTrace(); 看看究竟是什么异常
        assertEquals(200, result.getCode());
        assertEquals("登录有效", result.getMsg());
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
        assertEquals("当前设备已登出或会话已结束，请重新登录", result.getMsg());
    }

    @Test
    @DisplayName("刷新Token - 成功")
    void refreshToken_Success() {
        String rt = "valid_rt";
        String clientId = "pc";
        String uuid = "uuid-123";

        when(redisTools.getByKey("auth:rt:token:" + rt + ":" + clientId)).thenReturn(rt);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(uuid);

        User mockUser = new User();
        mockUser.setUsername("test_user");
        when(userMapper.selectOne(any())).thenReturn(mockUser);
        when(jwtUtils.createAccessToken(uuid, "test_user")).thenReturn("new_at");

        Result<Serializable> result = loginService.refreshToken(rt, clientId);

        assertEquals(ResultEnum.SUCCESS.getCode(), result.getCode());
        assertEquals("new_at", result.getData());
    }

    // ================= kickOut (按 UUID 踢出) 测试 =================
    @Test
    void kickOut_UserNotFound() {
        // 模拟数据库查无此人
        when(userMapper.selectCount(any())).thenReturn(0L);

        Result<Serializable> result = loginService.kickOutByUuid("invalid-uuid");

        assertEquals(404, result.getCode());
        assertEquals("未找到用户", result.getMsg());
        verify(stringRedisTemplate, never()).keys(anyString()); // 确保直接拦截，没查 Redis
    }

    @Test
    void kickOut_Success_WithActiveTokens() {
        // 模拟用户存在
        when(userMapper.selectCount(any())).thenReturn(1L);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        // 模拟 Redis 中存在该用户的 Token
        Set<String> keys = new HashSet<>(java.util.Collections.singletonList("auth:rt:user:uuid-123:pc"));
        when(stringRedisTemplate.keys(anyString())).thenReturn(keys);
        when(valueOperations.get("auth:rt:user:uuid-123:pc")).thenReturn("old_rt");

        Result<Serializable> result = loginService.kickOutByUuid("uuid-123");

        assertEquals(200, result.getCode());
        assertEquals("踢出成功", result.getMsg());
        // 验证确实执行了双向删除
        verify(stringRedisTemplate).delete("auth:rt:token:old_rt:pc");
        verify(stringRedisTemplate).delete("auth:rt:user:uuid-123:pc");
    }

    @Test
    void kickOut_Success_AlreadyOffline() {
        when(userMapper.selectCount(any())).thenReturn(1L);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        // 模拟 Redis 中找不到任何该用户的登录凭证
        when(stringRedisTemplate.keys(anyString())).thenReturn(new java.util.HashSet<>());

        Result<Serializable> result = loginService.kickOutByUuid("uuid-123");

        assertEquals(200, result.getCode());
        assertEquals("未找到用户的登录状态或已经踢出", result.getMsg());
    }

    // ================= kickOutByUsername (按 Username 踢出) 测试 =================
    @Test
    void kickOutByUsername_NotFound() {
        when(userMapper.selectOne(any())).thenReturn(null);

        Result<Serializable> result = loginService.kickOutByUsername("ghost_user");

        assertEquals(404, result.getCode());
        assertEquals("未找到用户", result.getMsg());
    }

    @Test
    void kickOutByUsername_Success() {
        // 模拟通过 username 查到了对应的 user
        User mockUser = new User();
        mockUser.setUuid("uuid-123");
        when(userMapper.selectOne(any())).thenReturn(mockUser);

        // 模拟底层 kickOut 所需的数据
        when(userMapper.selectCount(any())).thenReturn(1L);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(stringRedisTemplate.keys(anyString())).thenReturn(new java.util.HashSet<>());

        Result<Serializable> result = loginService.kickOutByUsername("real_user");

        assertEquals(200, result.getCode());
    }

    // ================= logout (主动登出) 测试 =================
    @Test
    void logout_Success() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        // 模拟找到了用户的登录 RT
        when(valueOperations.get("auth:rt:user:uuid-123:pc")).thenReturn("old_rt");

        Result<Serializable> result = loginService.logout("uuid-123", "pc");

        assertEquals(200, result.getCode());
        assertEquals("登出成功", result.getMsg());
        // 验证执行了双向删除
        verify(stringRedisTemplate).delete("auth:rt:token:old_rt:pc");
        verify(stringRedisTemplate).delete("auth:rt:user:uuid-123:pc");
    }

    @Test
    void logout_AlreadyOffline() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        // 模拟用户本身就没登录或已过期
        when(valueOperations.get(anyString())).thenReturn(null);

        Result<Serializable> result = loginService.logout("uuid-123", "pc");

        assertEquals(403, result.getCode());
        assertEquals("未找到登录状态或已下线", result.getMsg());
    }
}