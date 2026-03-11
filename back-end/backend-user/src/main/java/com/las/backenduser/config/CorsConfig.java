package com.las.backenduser.config;

import org.jspecify.annotations.NonNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 全局跨域配置
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.addAllowedOriginPattern("*");
        config.setAllowCredentials(true);
        config.addAllowedMethod("*");
        config.addAllowedHeader("*");
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }

    /**
     * 在 MVC 层为 springdoc/Swagger 路径额外注册 CORS，
     * 确保 Swagger UI 从外部域名访问时不会因混合内容或 CORS 预检失败。
     */
    @Bean
    public WebMvcConfigurer swaggerCorsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(@NonNull CorsRegistry registry) {
                registry.addMapping("/v3/api-docs/**")
                        .allowedOriginPatterns("*")
                        .allowedMethods("GET")
                        .allowedHeaders("*");
                registry.addMapping("/swagger-ui/**")
                        .allowedOriginPatterns("*")
                        .allowedMethods("GET")
                        .allowedHeaders("*");
            }
        };
    }
}
