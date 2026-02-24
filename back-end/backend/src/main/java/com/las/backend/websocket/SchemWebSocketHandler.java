package com.las.backend.websocket;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.las.backend.model.projectManager.WsProtocol;
import com.las.backend.service.projectManager.impl.WsServerServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Slf4j
@Component
@RequiredArgsConstructor
public class SchemWebSocketHandler extends TextWebSocketHandler {

    private final WsServerServiceImpl wsServerService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("WebSocket 已建立连接，Session ID: {}", session.getId());
        wsServerService.setSession(session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        log.info("收到来自 Mod 的消息: {}", payload);

        WsProtocol response = objectMapper.readValue(payload, WsProtocol.class);

        if ("RESULT".equals(response.action) || "MATERIAL_RESULT".equals(response.action)) {
            wsServerService.onResultReceived(response.id, response.data);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, org.springframework.web.socket.CloseStatus status) throws Exception {
        log.warn("WebSocket 连接已断开，Session ID: {}, 状态: {}", session.getId(), status);
    }
}