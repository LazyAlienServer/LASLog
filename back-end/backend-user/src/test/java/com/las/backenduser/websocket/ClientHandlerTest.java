package com.las.backenduser.websocket;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
class ClientHandlerTest {

    private ClientHandler clientHandler;
    private WebSocketSession mockSession;

    @BeforeEach
    void setUp() {
        clientHandler = new ClientHandler();
        mockSession = Mockito.mock(WebSocketSession.class);
    }

    @Test
    void testSendSuccess() throws IOException {
        // 1. 模拟连接建立
        when(mockSession.isOpen()).thenReturn(true);
        clientHandler.afterConnectionEstablished(mockSession);

        // 2. 执行发送
        clientHandler.send("Hello Minecraft!");

        // 3. 验证是否调用了发送方法
        verify(mockSession, times(1)).sendMessage(any(TextMessage.class));
    }

    @Test
    void testSendWhenSessionClosed() throws IOException {
        // 1. 模拟连接已关闭
        when(mockSession.isOpen()).thenReturn(false);
        clientHandler.afterConnectionEstablished(mockSession);

        // 2. 执行发送
        clientHandler.send("Hello?");

        // 3. 验证是否【没有】调用发送方法（因为 isOpen 是 false）
        verify(mockSession, never()).sendMessage(any(TextMessage.class));
    }

    @Test
    void testSendTriggerIOException() throws IOException {
        // 1. 模拟 Session 是开启状态
        when(mockSession.isOpen()).thenReturn(true);
        clientHandler.afterConnectionEstablished(mockSession);

        // 2. 关键：强制让 sendMessage 抛出 IOException
        doThrow(new IOException("模拟网络异常")).when(mockSession).sendMessage(any(TextMessage.class));

        // 3. 执行发送，此时会进入 catch 块
        clientHandler.send("测试异常路径");

        // 验证：虽然抛出了异常，但程序没有崩溃（被 catch 住了）
        verify(mockSession).sendMessage(any(TextMessage.class));
    }

    @Test
    void testAfterConnectionClosed() {
        // 1. 先建立连接
        clientHandler.afterConnectionEstablished(mockSession);

        // 2. 手动触发关闭逻辑
        clientHandler.afterConnectionClosed(mockSession, org.springframework.web.socket.CloseStatus.NORMAL);

        // 3. 验证 session 是否已清空
        // 尝试发送，如果 session 为空，内部会执行 log.warn("【WS发送丢弃】...")
        clientHandler.send("断开后的消息");

        // 验证：sendMessage 绝对不会被执行，因为 session 已经是 null 了
        try {
            verify(mockSession, never()).sendMessage(any());
        } catch (IOException e) {
            // 忽略
        }
    }
}