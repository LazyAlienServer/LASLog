package com.las.backenduser.websocket;

import com.las.backenduser.model.config.WsConfigProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WsClientServiceTest {

    @Mock
    private WsConfigProperties wsConfig;

    @Mock
    private ClientHandler clientHandler;

    @InjectMocks
    private WsClientService wsClientService;

    private final String testURL = "ws://localhost:8080/ws";
    private final String testToken = "test-token-123";

    @BeforeEach
    void setUp() {
        // 每个测试前清除 mock 状态
    }

    @Test
    @DisplayName("测试成功连接流程")
    void testConnect_Success() {
        // 1. 准备 Mock 数据
        when(wsConfig.getUrl()).thenReturn(testURL);
        when(wsConfig.getToken()).thenReturn(testToken);

        // 2. 执行方法
        wsClientService.connect();

        // 3. 验证是否读取了配置
        verify(wsConfig, times(1)).getUrl();
        verify(wsConfig, times(1)).getToken();

        // 注意：由于 StandardWebSocketClient 是在方法内 new 出来的，
        // 且 execute 是异步的，这里主要验证逻辑走通，没有抛出未捕获异常
    }

    @Test
    @DisplayName("测试连接异常流程 (覆盖 catch 块)")
    void testConnect_Exception() {
        // 1. 模拟异常：当调用 getUrl 时抛出运行时异常
        // 这将直接导致 try 块中断并进入catch
        when(wsConfig.getUrl()).thenThrow(new RuntimeException("模拟配置读取异常"));

        // 2. 执行方法
        // 虽然内部报错，但由于有 catch 块，测试方法本身不会抛出异常
        wsClientService.connect();

        // 3. 验证：确保即使报错，程序也尝试去获取了 URL
        verify(wsConfig, times(1)).getUrl();

        // 此时，你的代码覆盖率工具会显示 catch 块内的 log.error 行为已被覆盖
    }

    @Test
    @DisplayName("测试 URL 格式非法导致的异常 (覆盖 catch 块)")
    void testConnect_InvalidUri_Exception() {
        // 1. 提供一个非法的 URL 字符串，让 URI.create 报错
        when(wsConfig.getUrl()).thenReturn("not a valid uri ^_^");
        when(wsConfig.getToken()).thenReturn(testToken);

        // 2. 执行
        wsClientService.connect();

        // 3. 验证
        verify(wsConfig).getUrl();
        // URI.create("...") 抛出的 IllegalArgumentException 会被 catch 捕获
    }
}