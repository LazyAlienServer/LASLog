package com.las.backend.config;

import com.las.backend.websocket.SchemWebSocketHandler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

@Configuration
@EnableWebSocket
@ConditionalOnWebApplication
public class WebSocketConfig implements WebSocketConfigurer {

    private final SchemWebSocketHandler schmWebSocketHandler;

    public WebSocketConfig(SchemWebSocketHandler schmWebSocketHandler) {
        this.schmWebSocketHandler = schmWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(schmWebSocketHandler, "/ws").setAllowedOrigins("*");
    }
    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();

        // 将最大文本消息缓冲区大小设置为 5MB (5 * 1024 * 1024 字节)
        container.setMaxTextMessageBufferSize(10* 1024 * 1024);

        // 可选：同时加大二进制消息的缓冲区
        container.setMaxBinaryMessageBufferSize(10* 1024 * 1024);

        // 可选：设置最大会话空闲时间（毫秒），防止连接意外假死（比如 15 分钟）
        container.setMaxSessionIdleTimeout(900000L);

        return container;
    }
}