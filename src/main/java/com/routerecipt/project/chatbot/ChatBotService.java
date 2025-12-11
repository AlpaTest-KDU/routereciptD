package com.routerecipt.project.chatbot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class ChatBotService {

    private final RestTemplate restTemplate;

    public ChatBotService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public String ask(String question) {

        String url = "https://api.openai.com/v1/chat/completions";

        Map<String, Object> req = new HashMap<>();
        req.put("model", "gpt-4o-mini");  // 반드시 포함

        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of(
                "role", "user",
                "content", question
        ));

        req.put("messages", messages);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth("DUMMY");   // 인터셉터에서 교체됨.

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(req, headers);

        ResponseEntity<ChatResponse> response =
                restTemplate.postForEntity(url, entity, ChatResponse.class);

        return response.getBody().getChoices().get(0).getMessage().getContent();
    }
}
