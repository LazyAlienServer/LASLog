package com.las.backenduser.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                // 1. 设置连接超时：使用新的 connectTimeout 方法
                .connectTimeout(Duration.ofSeconds(5))
                // 2. 设置读取超时：使用新的 readTimeout 方法
                .readTimeout(Duration.ofSeconds(10))
                .build();
    }
}