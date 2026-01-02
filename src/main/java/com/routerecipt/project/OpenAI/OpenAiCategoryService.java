package com.routerecipt.project.OpenAI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.routerecipt.project.dto.AiCategoryRequest;
import com.routerecipt.project.dto.AiCategoryResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OpenAiCategoryService {

    private static final Logger log =
            LoggerFactory.getLogger(OpenAiCategoryService.class);

    private final RestTemplate restTemplate;

    @Value("${AI_CATEGORY_URL}")
    private String fastApiBaseUrl;

    /**
     * 단건 item 분류
     */
    public AiCategoryResponse classifyItem(String itemName) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            AiCategoryRequest requestBody = new AiCategoryRequest();
            requestBody.setText(itemName);

            HttpEntity<AiCategoryRequest> request =
                    new HttpEntity<>(requestBody, headers);

            String url = fastApiBaseUrl + "/predict";

            ResponseEntity<String> response =
                    restTemplate.postForEntity(url, request, String.class);

            log.error("RAW AI RESPONSE = {}", response.getBody());

            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(response.getBody(), AiCategoryResponse.class);

        } catch (Exception e) {
            log.error("AI CALL FAILED", e);
            return fallback();
        }
    }

    private AiCategoryResponse fallback() {
        AiCategoryResponse r = new AiCategoryResponse();
        r.setCategory("ETC");
        r.setConfidence(0.0);
        r.setSource("FALLBACK");
        return r;
    }
}
