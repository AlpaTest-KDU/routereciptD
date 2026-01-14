package com.routerecipt.project;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.routerecipt.project.mapper")
public class RouteReciptApplication {

	public static void main(String[] args) {
		SpringApplication.run(RouteReciptApplication.class, args);
	}

}
