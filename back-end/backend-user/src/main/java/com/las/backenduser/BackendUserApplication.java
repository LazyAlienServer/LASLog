package com.las.backenduser;

import com.las.backenduser.websocket.WsClientService;
import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@MapperScan("com.las.backenduser.mapper")
@Slf4j
public class BackendUserApplication {

	public static void main(String[] args) {
		SpringApplication.run(BackendUserApplication.class, args);
	}

	/**
	 * 自动触发连接
	 * @param clientService ws Service
	 * @return CommandLineRunner
	 */
	@Bean
	public CommandLineRunner runner(WsClientService clientService) {
		return args -> {
			log.info("【系统启动】: 准备建立 WebSocket 初始连接...");
			clientService.connect();
			log.info("----------WS START----------");
		};
	}

}
