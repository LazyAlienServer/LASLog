package com.las.backend.config;

import com.las.backend.websocket.SchemWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final SchemWebSocketHandler schmWebSocketHandler;

    public WebSocketConfig(SchemWebSocketHandler schmWebSocketHandler) {
        this.schmWebSocketHandler = schmWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(schmWebSocketHandler, "/ws").setAllowedOrigins("*");
    }
}