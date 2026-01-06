package com.routerecipt.project.config;

import okhttp3.OkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * OpenAI HTTP Client 설정 클래스
 *
 * 역할:
 *  - OpenAI API 호출에 사용할 OkHttpClient를 Spring Bean으로 등록
 *  - 네트워크 타임아웃 등 공통 설정을 중앙에서 관리
 */
@Configuration
public class OpenAiConfig {
	
	/**
     * OpenAI 전용 OkHttpClient Bean
     *
     * @return OkHttpClient (timeout 설정 포함)
     */
	@Bean
    public OkHttpClient openAiHttpClient() {
        return new OkHttpClient.Builder()
        		// 서버 연결 시도 최대 대기 시간
                .connectTimeout(Duration.ofSeconds(10))
                
                // 요청 후 응답을 기다리는 최대 시간
                .readTimeout(Duration.ofSeconds(60))
                
                // OkHttpClient 생성
                .build();
    }
}
