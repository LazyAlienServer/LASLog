package com.las.backenduser.websocket;

import com.las.backenduser.model.config.WsConfigProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import java.net.URI;

@Slf4j
@Service
public class WsClientService {

    private final WsConfigProperties wsConfig;
    private final ClientHandler clientHandler;

    public WsClientService(ClientHandler clientHandler, WsConfigProperties wsConfigProperties) {
        this.clientHandler = clientHandler;
        this.wsConfig = wsConfigProperties;
    }

    public void connect() {
        try {
            String url = wsConfig.getUrl();
            String token = wsConfig.getToken();

            log.info("从配置类读取到的 URL: {}", url);


            StandardWebSocketClient client = new StandardWebSocketClient();

            // 配置 Header
            WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
            headers.add("Authorization", "Bearer " + token);

            log.info("【WS系统】: 开始连接 {}", url);
            // 异步执行连接
            client.execute(clientHandler, headers, URI.create(url));

        } catch (Exception e) {
            log.error("【WS系统】: 启动连接异常", e);
        }
    }
}
