package com.las.backenduser.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis 统一配置类
 */
@Configuration
@MapperScan("com.las.backenduser.mapper")
public class MybatisConfig {
}