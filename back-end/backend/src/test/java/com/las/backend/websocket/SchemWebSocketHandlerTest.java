package com.las.backend.websocket;

import com.las.backend.service.projectmanager.impl.WsServerServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.verify;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SchemWebSocketHandlerTest {

    @Mock
    private WsServerServiceImpl wsServerService; // 模拟你的核心发信服务

    @Mock
    private WebSocketSession session; // 模拟 WebSocket 连接

    private SchemWebSocketHandler handler;

    @BeforeEach
    void setUp() {
        // 使用构造器注入
        handler = new SchemWebSocketHandler(wsServerService);
    }

    @Test
    void afterConnectionEstablished() throws Exception {
        when(session.getId()).thenReturn("test-session-123");

        // 模拟建立连接
        handler.afterConnectionEstablished(session);

        // 验证是否把 session 存进了服务中
        verify(wsServerService).setSession(session);
    }

    @Test
    void handleTextMessage_Result() throws Exception {
        // 模拟 Mod 发回的 JSON 消息 (进度查询结果)
        String mockPayload = "{\"id\":\"uuid-123\", \"action\":\"RESULT\", \"data\":\"{\\\"correct\\\":10}\"}";
        TextMessage message = new TextMessage(mockPayload);

        // 模拟收到消息
        handler.handleTextMessage(session, message);

        // 验证是否成功解析，并调用了 onResultReceived
        verify(wsServerService).onResultReceived(eq("uuid-123"), anyString());
    }

    @Test
    void handleTextMessage_MaterialResult() throws Exception {
        // 模拟 Mod 发回的 JSON 消息 (材料查询结果)
        String mockPayload = "{\"id\":\"uuid-456\", \"action\":\"MATERIAL_RESULT\", \"data\":\"{}\"}";
        TextMessage message = new TextMessage(mockPayload);

        handler.handleTextMessage(session, message);

        verify(wsServerService).onResultReceived(eq("uuid-456"), anyString());
    }

    @Test
    void handleTextMessage_SchemFilesInfo() throws Exception {
        // 模拟收到 SCHEMAITC_FILES_INFO 类型的消息
        String mockPayload = "{\"id\":\"uuid-999\", \"action\":\"SCHEMAITC_FILES_INFO\", \"data\":\"[{\\\"name\\\":\\\"Test\\\"}]\"}";
        TextMessage message = new TextMessage(mockPayload);

        // 调用目标方法
        handler.handleTextMessage(session, message);

        // 验证确实成功走进了 if 分支，并提取了正确的 id 和 data 传给 service
        verify(wsServerService).onResultReceived("uuid-999", "[{\"name\":\"Test\"}]");
    }

    @Test
    void handleTextMessage_OtherAction() throws Exception {
        // 模拟收到无关的消息，测试 if 分支进不去的情况
        String mockPayload = "{\"id\":\"uuid-789\", \"action\":\"UNKNOWN\", \"data\":\"\"}";
        TextMessage message = new TextMessage(mockPayload);

        handler.handleTextMessage(session, message);

        // 验证绝对没有调用 onResultReceived
        verify(wsServerService, Mockito.never()).onResultReceived(anyString(), anyString());
    }

    @Test
    void afterConnectionClosed() {
        when(session.getId()).thenReturn("test-session-123");

        // 方法 1：断言这个方法执行过程中不会抛出任何异常
        assertDoesNotThrow(() -> handler.afterConnectionClosed(session, CloseStatus.NORMAL));

        // 方法 2：验证确实调用了 session 的 getId() 方法来打印日志
        verify(session).getId();
    }
}