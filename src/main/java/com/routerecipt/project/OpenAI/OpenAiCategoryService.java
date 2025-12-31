package com.routerecipt.project.OpenAI;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.routerecipt.project.dto.AiCategoryResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OpenAiCategoryService {

    private final RestTemplate restTemplate;

    private static final String FAST_API_URL =
            "http://192.168.0.123:8000/predict";

    /**
     * 단건 item 분류
     */
    public AiCategoryResponse classifyItem(String itemName) {

        if (itemName == null || itemName.isBlank()) {
            return fallback();
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String body = """
        {
          "text": "%s"
        }
        """.formatted(itemName.replace("\"", ""));

        HttpEntity<String> request =
                new HttpEntity<>(body, headers);

        try {
            ResponseEntity<AiCategoryResponse> response =
                    restTemplate.postForEntity(
                            FAST_API_URL,
                            request,
                            AiCategoryResponse.class
                    );

            return response.getBody();

        } catch (Exception e) {
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
