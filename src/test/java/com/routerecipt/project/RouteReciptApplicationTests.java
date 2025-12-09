package com.routerecipt.project;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class RouteReciptApplicationTests {

	@Test
	@Disabled("CI 환경에서 DB/Redis 미사용으로 인해 테스트 비활성화")
	void contextLoads() {
	}

}
