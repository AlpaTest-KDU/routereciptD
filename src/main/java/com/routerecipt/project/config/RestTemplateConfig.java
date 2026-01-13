package com.routerecipt.project.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
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
    public RestTemplate clovaRestTemplate() {
        SimpleClientHttpRequestFactory factory =
            new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(15000);
        return new RestTemplate(factory);
    }
}

