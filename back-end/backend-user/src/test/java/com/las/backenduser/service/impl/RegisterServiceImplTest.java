package com.las.backenduser.service.impl;

import com.las.backenduser.mapper.UserMapper;
import com.las.backenduser.model.User;
import com.las.backenduser.model.dto.register.RegisterCompleteDTO;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegisterServiceImplTest {

    @Mock
    private UserMapper userMapper;
    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private RestTemplate restTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private RegisterServiceImpl registerService;

    private static final String SALT = "原神牛逼";

    @BeforeEach
    void setUp() {
        // 关键！强行将 mock 的 RestTemplate 注入进去
        ReflectionTestUtils.setField(registerService, "restTemplate", restTemplate);
    }

    // 辅助方法：手动生成一个测试 Token
    private String createTestToken(String qq, int direction, long expireMs, boolean corruptSignature, boolean corruptBody) {
        String base64Qq = Base64.getUrlEncoder().withoutPadding().encodeToString(qq.getBytes(StandardCharsets.UTF_8));
        String base64Expire = Base64.getUrlEncoder().withoutPadding().encodeToString(String.valueOf(expireMs).getBytes(StandardCharsets.UTF_8));
        String messageBody = corruptBody ? (base64Qq + "-" + direction) : (base64Qq + "-" + direction + "-" + base64Expire);

        String signature = DigestUtils.sha256Hex((messageBody + SALT).getBytes(StandardCharsets.UTF_8));
        if (corruptSignature) signature = "bad_signature";

        return messageBody + "." + signature;
    }

    // --- generateToken Tests ---
    @Test
    void generateToken() {
        String token = registerService.generateToken("123", 1, System.currentTimeMillis() + 10000);
        assertNotNull(token);
        assertTrue(token.contains("."));
    }

    // --- verifyAndDecodeToken Tests ---
    @Test
    void verifyAndDecodeToken_Success() {
        long future = System.currentTimeMillis() + 100000;
        String token = createTestToken("123", 1, future, false, false);
        Map<String, Object> result = registerService.verifyAndDecodeToken(token);

        assertEquals("123", result.get("qq"));
        assertEquals(1, result.get("direction"));
        assertEquals(future, result.get("expireMs"));
    }

    @Test
    void verifyAndDecodeToken_NullOrNoDot() {
        assertThrows(IllegalArgumentException.class, () -> registerService.verifyAndDecodeToken(null));
        assertThrows(IllegalArgumentException.class, () -> registerService.verifyAndDecodeToken("nodot-here"));
    }

    @Test
    void verifyAndDecodeToken_TooManyDots() {
        assertThrows(IllegalArgumentException.class, () -> registerService.verifyAndDecodeToken("part1.part2.part3"));
    }

    @Test
    void verifyAndDecodeToken_BadSignature() {
        String token = createTestToken("123", 1, System.currentTimeMillis() + 10000, true, false);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> registerService.verifyAndDecodeToken(token));
        assertEquals("Token签名校验失败，可能已被篡改", ex.getMessage());
    }

    @Test
    void verifyAndDecodeToken_BadBodyLength() {
        // 创建一个只有两段的 body，但是签名是合法的
        String token = createTestToken("123", 1, 0, false, true);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> registerService.verifyAndDecodeToken(token));
        assertEquals("Token消息体解析失败", ex.getMessage());
    }

    @Test
    void verifyAndDecodeToken_Expired() {
        long past = System.currentTimeMillis() - 10000;
        String token = createTestToken("123", 1, past, false, false);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> registerService.verifyAndDecodeToken(token));
        assertEquals("该邀请链接已过期", ex.getMessage());
    }

    @Test
    void verifyAndDecodeToken_Base64Exception() {
        // 构造一个签名合法，但 body 的第一部分是无效的 Base64 字符
        String messageBody = "%%%-" + 1 + "-MTIz";
        String signature = DigestUtils.sha256Hex((messageBody + SALT).getBytes(StandardCharsets.UTF_8));
        String token = messageBody + "." + signature;

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> registerService.verifyAndDecodeToken(token));
        assertEquals("Token Base64解码异常", ex.getMessage());
    }

    // --- checkMinecraftId Tests ---
    @Test
    void checkMinecraftId_Success() throws IOException {
        Map<String, String> mockResponse = new HashMap<>();
        mockResponse.put("id", "real-uuid-123");
        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenReturn(mockResponse);

        String uuid = registerService.checkMinecraftId("Notch");
        assertEquals("real-uuid-123", uuid);
    }

    @Test
    void checkMinecraftId_NotFound() throws IOException {
        when(restTemplate.getForObject(anyString(), eq(Map.class)))
                .thenThrow(HttpClientErrorException.NotFound.class); // 模拟 404
        assertNull(registerService.checkMinecraftId("Unknown"));
    }

    @Test
    void checkMinecraftId_OtherException() {
        when(restTemplate.getForObject(anyString(), eq(Map.class)))
                .thenThrow(new RuntimeException("API down"));
        assertThrows(IOException.class, () -> registerService.checkMinecraftId("Notch"));
    }

    // --- activateToken Tests ---
    @Test
    void activateToken_Success() {
        long future = System.currentTimeMillis() + 100000;
        String token = createTestToken("123", 1, future, false, false);

        when(stringRedisTemplate.hasKey(anyString())).thenReturn(false);

        HashMap<String, Object> map = registerService.activateToken(token);
        assertEquals("123", map.get("qq"));
        assertNull(map.get("expireMs")); // 验证清洗
    }

    @Test
    void activateToken_Blacklisted() {
        String token = createTestToken("123", 1, System.currentTimeMillis() + 100000, false, false);
        when(stringRedisTemplate.hasKey(anyString())).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> registerService.activateToken(token));
        assertEquals("该激活链接已被使用！", ex.getMessage());
    }

    // --- completeRegister Tests ---
    @Test
    void completeRegister_IncompleteInfo() {
        RegisterCompleteDTO dto = new RegisterCompleteDTO();
        assertThrows(IllegalArgumentException.class, () -> registerService.completeRegister(dto));

        dto.setToken("123"); // 只有 token 不行
        assertThrows(IllegalArgumentException.class, () -> registerService.completeRegister(dto));
    }

    @Test
    void completeRegister_Blacklisted() {
        RegisterCompleteDTO dto = new RegisterCompleteDTO();
        dto.setToken(createTestToken("123", 1, System.currentTimeMillis() + 10000, false, false));
        dto.setMinecraftId("Notch");
        dto.setPassword("pwd");

        when(stringRedisTemplate.hasKey(anyString())).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> registerService.completeRegister(dto));
    }

    @Test
    void completeRegister_MinecraftIdInvalid() {
        RegisterCompleteDTO dto = new RegisterCompleteDTO();
        dto.setToken(createTestToken("123", 1, System.currentTimeMillis() + 10000, false, false));
        dto.setMinecraftId("FakeUser");
        dto.setPassword("pwd");

        when(stringRedisTemplate.hasKey(anyString())).thenReturn(false);
        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenReturn(null); // 返回空，模拟找不到

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> registerService.completeRegister(dto));
        assertEquals("Minecraft ID 不存在或非正版", ex.getMessage());
    }

    @Test
    void completeRegister_PasswordNull() { // 顺便帮你补上了方法名拼写漏掉的 'p'
        RegisterCompleteDTO dto = new RegisterCompleteDTO();
        dto.setMinecraftId("a");
        dto.setUsername("1");
        dto.setToken(createTestToken("123", 1, System.currentTimeMillis() + 10000, false, false));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> registerService.completeRegister(dto));
        assertEquals("注册信息不完整", ex.getMessage());
    }

    @Test
    void testCheckMinecraftId_WhenResponseMissingId_ReturnsNull() throws IOException {
        String username = "SomeWeirdUser";
        String expectedUrl = "https://api.mojang.com/users/profiles/minecraft/" + username;

        // 捏造一个不包含 "id" 的 Map（比如 API 格式变了，或者返回了其他提示）
        Map<String, Object> mockResponse = new HashMap<>();
        mockResponse.put("errorMessage", "Something went wrong");

        when(restTemplate.getForObject(expectedUrl, Map.class));

        String result = registerService.checkMinecraftId(username);

        assertNull(result);
    }

    @Test
    void completeRegister_Success() throws IOException {
        RegisterCompleteDTO dto = new RegisterCompleteDTO();
        long future = System.currentTimeMillis() + 100000;
        dto.setToken(createTestToken("123", 1, future, false, false));
        dto.setMinecraftId("Notch");
        dto.setUsername("NotchUsername");
        dto.setPassword("pwd123");

        when(stringRedisTemplate.hasKey(anyString())).thenReturn(false);

        Map<String, String> mockResponse = new HashMap<>();
        mockResponse.put("id", "real-uuid-123");
        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenReturn(mockResponse);

        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        // 核心执行
        registerService.completeRegister(dto);

        // 验证数据库插入
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userMapper, times(1)).insert(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertEquals(123L, savedUser.getQq());
        assertEquals("real-uuid-123", savedUser.getMainMinecraftUuid());
        assertEquals("NotchUsername", savedUser.getUsername());
        assertEquals(1, savedUser.getStatus());

        // 验证 Redis 写入
        verify(valueOperations, times(1)).set(anyString(), eq("1"), anyLong(), eq(TimeUnit.MILLISECONDS));
    }

    @Test
    void completeRegister_RemainingMsZero_ShouldNotSetRedis() throws Exception {
        // 1. 使用 spy() 包装真实的 service，这样我们可以拦截它内部的方法调用
        RegisterServiceImpl spyService = spy(registerService);

        RegisterCompleteDTO dto = new RegisterCompleteDTO();
        dto.setToken("dummy.token"); // 随便传，因为解析步骤会被我们拦截掉
        dto.setMinecraftId("Notch");
        dto.setUsername("NotchUsername");
        dto.setPassword("pwd123");

        Map<String, Object> fakeUserInfo = new HashMap<>();
        fakeUserInfo.put("qq", "123");
        fakeUserInfo.put("signature", "dummy_signature");
        // 故意比当前时间少 1000 毫秒
        fakeUserInfo.put("expireMs", System.currentTimeMillis() - 1000);

        doReturn(fakeUserInfo).when(spyService).verifyAndDecodeToken(anyString());

        // 3. Mock 必要的外部依赖
        when(stringRedisTemplate.hasKey(anyString())).thenReturn(false);
        Map<String, String> mockResponse = new HashMap<>();
        mockResponse.put("id", "real-uuid-123");
        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenReturn(mockResponse);

        // 4. 执行核心方法（注意：这里一定要用 spyService 去调用！）
        spyService.completeRegister(dto);

        // 5. 断言：验证由于 remainingMs 为负数，Redis 的 opsForValue() 绝对没有被调用
        verify(stringRedisTemplate, never()).opsForValue();
    }
}