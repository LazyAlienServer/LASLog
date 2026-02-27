package com.las.backenduser;

import com.las.backenduser.websocket.WsClientService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BackendUserApplicationTests {

	@Mock
	private WsClientService wsClientService;

	/**
	 * 测试 main 方法
	 * 利用 MockedStatic 拦截 SpringApplication.run，防止真正启动 Spring 容器
	 */
	@Test
	void testMain() {
		String[] args = {"test-arg1", "test-arg2"};

		// 开启静态 Mock 作用域
		try (MockedStatic<SpringApplication> mockedSpringApp = Mockito.mockStatic(SpringApplication.class)) {
			// 执行 main 方法
			BackendUserApplication.main(args);

			// 验证 SpringApplication.run 是否被正确调用，且参数传递无误
			mockedSpringApp.verify(() -> SpringApplication.run(BackendUserApplication.class, args), times(1));
		}
	}

	/**
	 * 测试 runner 方法 (CommandLineRunner 注入与执行)
	 */
	@Test
	void testRunner() throws Exception {
		// 手动实例化启动类
		BackendUserApplication app = new BackendUserApplication();

		// 调用 runner 方法获取 CommandLineRunner 对象
		CommandLineRunner runner = app.runner(wsClientService);

		// 1. 断言 runner 对象不为空
		assertNotNull(runner);

		// 2. 核心：手动触发 runner 的 run 方法，以此覆盖 Lambda 表达式内部的代码
		runner.run("mocked", "args");

		// 3. 验证 WsClientService 的 connect() 方法确实被调用了 1 次
		verify(wsClientService, times(1)).connect();
	}
}