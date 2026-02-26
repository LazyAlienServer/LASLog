package com.las.backenduser.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;

@Slf4j
@Component
public class ClientHandler extends TextWebSocketHandler {
    private WebSocketSession session;

    // 【收数据】的地方
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        String payload = message.getPayload();
        log.info("【WS收到】: {}", payload);
    }

    // 【发数据】的方法
    public void send(String text) {
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(text));
                log.info("【WS发送】: {}", text);
            } catch (IOException e) {
                log.error("【WS发送失败】", e);
            }
        } else {
            log.warn("【WS发送丢弃】: 连接未建立或已断开");
        }
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        this.session = session;
        log.info("【WS系统】: 连接已建立, SessionId: {}", session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        this.session = null;
        log.warn("【WS系统】: 连接已断开, 原因: {}", status.getReason());
    }
}
