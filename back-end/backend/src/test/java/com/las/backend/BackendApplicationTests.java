package com.las.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@SpringBootTest
class BackendApplicationTests {

    @Test
    void contextLoads() {
        // 默认的上下文加载测试，验证 Spring 容器能否正常启动
    }

    @Test
    void testMain() {
        // 显式调用 main 方法以覆盖那行代码
        // 传入参数关闭 Web 环境，防止启动 Tomcat 导致端口冲突报错
        assertDoesNotThrow(() -> BackendApplication.main(new String[]{"--spring.main.web-application-type=none"}), "Spring Boot 主程序启动失败！");
    }
}