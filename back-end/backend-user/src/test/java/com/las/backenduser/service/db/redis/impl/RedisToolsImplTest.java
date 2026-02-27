package com.las.backenduser.service.db.redis.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

/**
 * RedisToolsImpl 单元测试
 * 目标：100% 覆盖率
 */
@ExtendWith(MockitoExtension.class)
class RedisToolsImplTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private RedisToolsImpl redisTools;

    @BeforeEach
    void setUp() {
        // 因为大部分方法都要用到 opsForValue()，所以提前配置 mock 行为
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("测试带过期时间的插入 - 覆盖 insert(key, value, timeout, unit)")
    void testInsertWithTimeout() {
        String key = "test:at:1";
        String value = "access_token_value";
        long timeout = 14;
        TimeUnit unit = TimeUnit.DAYS;

        redisTools.insert(key, value, timeout, unit);

        // 验证是否真的调用了底层的 set 方法，参数是否一致
        verify(valueOperations, times(1)).set(key, value, timeout, unit);
    }

    @Test
    @DisplayName("测试无过期时间的插入 - 覆盖 insert(key, value)")
    void testInsertWithoutTimeout() {
        String key = "test:config";
        String value = "some_config";

        redisTools.insert(key, value);

        // 验证底层调用
        verify(valueOperations, times(1)).set(key, value);
    }

    @Test
    @DisplayName("测试根据 Key 获取数据 - 覆盖 getByKey(key)")
    void testGetByKey() {
        String key = "user:1001";
        String expectedValue = "{\"id\":1001, \"name\":\"sunyinuo\"}";

        // 模拟返回值
        when(valueOperations.get(key)).thenReturn(expectedValue);

        Object actualValue = redisTools.getByKey(key);

        assertEquals(expectedValue, actualValue);
        verify(valueOperations, times(1)).get(key);
    }

    @Test
    @DisplayName("测试删除数据 - 覆盖 delete(key)")
    void testDelete() {
        String key = "auth:rt:temp";

        redisTools.delete(key);

        // 验证 redisTemplate 自身的 delete 是否被触发
        verify(redisTemplate, times(1)).delete(key);
    }
}