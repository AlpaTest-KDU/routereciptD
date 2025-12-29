package com.routerecipt.project.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.openai.client.OpenAIClient;
import com.openai.client.OpenAIClientImpl;

@Configuration
public class OpenAiConfig {
	

    @Value("${openai.api-key}")
    private String apiKey;

    @Bean
    public OpenAIClient openAIClient() {
        OpenAIClientOptions options = OpenAIClientOptions.builder()
                .apiKey(apiKey)
                .build();

        return new OpenAIClientImpl(options);
    }
}
