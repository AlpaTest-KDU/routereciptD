package com.routerecipt.project.chatbot;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;


/**
 * OpenAI ChatBot 설정 클래스
 *
 * 역할:
 *  - OpenAI API Key 주입
 *  - OpenAIClient를 Spring Bean으로 등록
 *
 * 이 Bean은 ChatBotService, Controller 등에서
 * 의존성 주입(@Autowired / 생성자 주입)으로 사용됨
 */
@Configuration
public class ChatBotConfig {
	
	/**
     * OpenAI API Key
     * (application.yml / properties / 환경변수에서 주입)
     */
    @Value("${openai.api-key}")
    private String apiKey;
    
    
    /**
     * OpenAIClient Bean 등록
     *
     * @return OpenAIClient (OkHttp 기반 구현체)
     */
    @Bean
    OpenAIClient openAIClient() {
        return OpenAIOkHttpClient.builder()
                .apiKey(apiKey)
                .build();
                
    }
}
