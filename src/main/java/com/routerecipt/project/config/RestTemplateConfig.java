package com.routerecipt.project.config;

import java.time.Duration;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;


/**
 * RestTemplate 설정 클래스
 *
 * 역할:
 *  - 외부 API 호출에 사용할 RestTemplate를 Spring Bean으로 등록
 *  - 공통 타임아웃 정책 적용
 */
@Configuration
public class RestTemplateConfig {
	
	/**
     * RestTemplate Bean
     *
     * @param builder Spring Boot가 제공하는 RestTemplateBuilder
     * @return timeout 설정이 적용된 RestTemplate
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
        	// 서버 연결 시도 최대 대기 시간
            .setConnectTimeout(Duration.ofSeconds(2))
            
            // 요청 후 응답을 기다리는 최대 시간
            .setReadTimeout(Duration.ofSeconds(3))
            
            // RestTemplate 생성
            .build();
    }
}

