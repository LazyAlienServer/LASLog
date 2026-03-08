package com.las.backend.service.projectmanager.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.las.backend.model.projectmanager.WsProtocol;
import com.las.backend.service.projectmanager.WsServerService;
import lombok.Setter;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
public class WsServerServiceImpl implements WsServerService{

    private final Map<String, CompletableFuture<String>> pendingRequests = new ConcurrentHashMap<>();

    @Setter
    private WebSocketSession session;

    @Override
    public CompletableFuture<String> sendAndAwait(String action, String data) throws IOException {
        String requestId = UUID.randomUUID().toString();
        CompletableFuture<String> future = new CompletableFuture<>();
        pendingRequests.put(requestId, future);

        WsProtocol msg = new WsProtocol(requestId, action, data);
        ObjectMapper mapper = new ObjectMapper();
        session.sendMessage(new TextMessage(mapper.writeValueAsString(msg)));

        return future.orTimeout(10, TimeUnit.SECONDS);
    }

    @Override
    public void onResultReceived(String id, String result) {
        CompletableFuture<String> future = pendingRequests.remove(id);
        if (future != null) {
            future.complete(result);
        }
    }
}
