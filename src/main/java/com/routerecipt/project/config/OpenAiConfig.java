package com.routerecipt.project.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class OpenAiConfig {

	@Bean
	public WebClient openAiWebClient(WebClient.Builder builder) {
        return builder
                .baseUrl("https://api.openai.com/v1")
                .build();
    }
}
