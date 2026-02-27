package com.las.backenduser.service.impl;

import com.las.backenduser.mapper.UserMapper;
import com.las.backenduser.model.User;
import com.las.backenduser.service.db.redis.impl.RedisToolsImpl;
import com.las.backenduser.utils.jwt.JwtUtils;
import com.las.backenduser.utils.result.Result;
import com.las.backenduser.utils.result.ResultEnum;
import com.las.backenduser.utils.salt.Salt;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoginServiceImplTest {

    @Mock
    private RedisToolsImpl redisTools;

    @Mock
    private UserMapper userMapper;

    @Mock
    private JwtUtils jwtUtils;

    @InjectMocks
    private LoginServiceImpl loginService;

    @Test
    @DisplayName("分支1：登录成功 - 覆盖所有成功路径代码")
    void login_Success() {
        // 1. 准备数据
        String userName = "sunyinuo";
        String passwd = "correct_password";
        String clientId = "pc_web";
        String salt = "random_salt";
        String hashedPw = "hashed_password";
        String uuid = "user-uuid-001";

        User mockUser = new User();
        mockUser.setUsername(userName);
        mockUser.setPassword(hashedPw);
        mockUser.setSalt(salt);
        mockUser.setUuid(uuid);

        // 2. Mock 行为
        when(userMapper.selectOne(any())).thenReturn(mockUser);
        when(jwtUtils.createRefreshToken()).thenReturn("mock_rt");
        when(jwtUtils.createAccessToken(uuid, userName)).thenReturn("mock_at");

        // 使用 mockStatic 处理静态工具类 Salt (Mockito 3.4+ 支持)
        try (MockedStatic<Salt> saltMockedStatic = mockStatic(Salt.class)) {
            saltMockedStatic.when(() -> Salt.salt(passwd, salt)).thenReturn(hashedPw);

            // 3. 执行
            Result<String> result = loginService.login(userName, passwd, clientId);

            // 4. 断言与验证
            assertEquals(ResultEnum.SUCCESS.getCode(), result.getCode());
            assertTrue(result.getData().contains("AT"));
            assertTrue(result.getData().contains("RT"));

            // 验证 Redis 是否存入
            verify(redisTools).insert("auth:rt:" + uuid + ":" + clientId, "mock_rt", 14L, TimeUnit.DAYS);
        }
    }

    @Test
    @DisplayName("分支2：用户名错误 - 覆盖 getUserByName == null 路径")
    void login_UserNotFound() {
        String userName = "wrong_user";
        when(userMapper.selectOne(any())).thenReturn(null);

        Result<String> result = loginService.login(userName, "any_password", "any_client");

        assertEquals(ResultEnum.UNAUTHORIZED.getCode(), result.getCode());
        assertEquals("用户名或密码错误", result.getMsg());
        // 验证后续逻辑未执行
        verifyNoInteractions(jwtUtils, redisTools);
    }

    @Test
    @DisplayName("分支3：密码错误 - 覆盖 Objects.equals(...) == false 路径")
    void login_WrongPassword() {
        // 1. 准备数据
        String userName = "sunyinuo";
        String wrongPass = "wrong_password";
        String salt = "some_salt";

        User mockUser = new User();
        mockUser.setUsername(userName);
        mockUser.setSalt(salt);
        mockUser.setPassword("correct_hash_in_db");

        // 2. Mock 行为
        when(userMapper.selectOne(any())).thenReturn(mockUser);

        try (MockedStatic<Salt> saltMockedStatic = mockStatic(Salt.class)) {
            // 模拟加盐后与数据库不匹配
            saltMockedStatic.when(() -> Salt.salt(eq(wrongPass), anyString())).thenReturn("wrong_hash");

            // 3. 执行
            Result<String> result = loginService.login(userName, wrongPass, "any_client");

            // 4. 断言
            assertEquals(ResultEnum.UNAUTHORIZED.getCode(), result.getCode());
            assertEquals("用户名或密码错误", result.getMsg());
            verifyNoInteractions(jwtUtils, redisTools);
        }
    }
}