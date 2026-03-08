package com.las.backenduser.model.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "remote.ws") // 对应 yml 中的前缀
public class WsConfigProperties {
    private String url;
    private String token;
}
