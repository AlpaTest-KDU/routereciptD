package com.routerecipt.project.OpenAI;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;


/**
 * WebClient 설정 클래스
 *
 * 역할:
 *  - AI 서버(FastAPI 등)와 통신하기 위한 WebClient를 Bean으로 등록
 *  - 공통 baseUrl 및 기본 설정을 한 곳에서 관리
 */
@Configuration
public class WebClientConfig {
	
	// AI 서버 전용 WebClient Bean
	@Bean
	public WebClient aiWebClient() {
		return WebClient.builder()
				.baseUrl("http://127.0.0.1:8000")
				.build();
	}
}
