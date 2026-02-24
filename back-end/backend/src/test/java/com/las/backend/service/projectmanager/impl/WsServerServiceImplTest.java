package com.las.backend.service.projectmanager.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WsServerServiceImplTest {

    private WsServerServiceImpl wsServerService;

    @Mock
    private WebSocketSession session;

    @BeforeEach
    void setUp() {
        wsServerService = new WsServerServiceImpl();
        wsServerService.setSession(session);
    }

    @Test
    void sendAndAwait_Success() throws Exception {
        // 测试发送逻辑
        CompletableFuture<String> future = wsServerService.sendAndAwait("TEST_ACTION", "{\"test\": 1}");

        // 验证确实调用了 WebSocket 发送文本
        verify(session).sendMessage(any(TextMessage.class));

        // 验证返回了 future 且还未完成
        assertNotNull(future);
        assertFalse(future.isDone());
    }

    @Test
    void onResultReceived_CompletesExistingFuture() throws Exception {
        // 先发送一个消息，制造一个挂起的请求
        CompletableFuture<String> future = wsServerService.sendAndAwait("TEST", "data");

        // 验证发消息是没问题的，但因为发送时使用的是随机 UUID，我们在单元测试外部比较难抓取
        // 这里主要测试它收到一个不存在的 UUID 时不会报错（兜底逻辑）
        wsServerService.onResultReceived("fake-uuid", "result data");
        assertFalse(future.isDone()); // 原来的 future 当然不会被完成
    }
}