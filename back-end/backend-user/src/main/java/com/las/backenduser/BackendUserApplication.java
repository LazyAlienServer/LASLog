package com.las.backenduser;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.las.backenduser.mapper")
public class BackendUserApplication {

	public static void main(String[] args) {
		SpringApplication.run(BackendUserApplication.class, args);
	}

}
