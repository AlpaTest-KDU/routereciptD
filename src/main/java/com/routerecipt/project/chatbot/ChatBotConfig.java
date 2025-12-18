package com.routerecipt.project.chatbot;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

@Configuration
public class ChatBotConfig {

	@Value("${openai.api-key}")
    private String apiKey;

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .additionalInterceptors((request, body, execution) -> {

                    request.getHeaders().setContentType(MediaType.APPLICATION_JSON);
                    request.getHeaders().setBearerAuth(apiKey);

                    return execution.execute(request, body);
                })
                .build();
    }
}
