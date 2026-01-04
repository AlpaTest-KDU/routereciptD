package com.routerecipt.project.OpenAI;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;

import jakarta.annotation.PostConstruct;
import reactor.core.publisher.Mono;

@Service
public class OpenAiOcrAssisService {

    private WebClient webClient;

    @Value("${openai.api-key}")
    private String apiKey;

    @Value("${openai.endpoint:https://api.openai.com/v1}")
    private String endpoint;

    @Value("${openai.model:gpt-4.1-mini}")
    private String model;

    @Value("${openai.timeout-ms:60000}")
    private long timeoutMs;

    @PostConstruct
    public void init() {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("openai.api-key 가 비어있습니다.");
        }

        this.webClient = WebClient.builder()
                .baseUrl(endpoint)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey.trim())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /**
     * OCR 누락 필드 보정 (Vision + OCR TEXT)
     * 🔹 [기존 로직 그대로 유지]
     */
    public OpenAiReceiptResult fixMissingFieldsWithVision(
            MultipartFile file,
            String rawText
    ) {

        try {
            byte[] bytes = file.getBytes();
            String base64 = java.util.Base64.getEncoder().encodeToString(bytes);
            String mime = (file.getContentType() != null)
                    ? file.getContentType()
                    : "image/jpeg";

            String prompt = """
            다음은 한국 영수증이다.
            이미지와 OCR 텍스트를 참고하여 누락된 값을 추론하라.
            반드시 아래 스키마의 "순수 JSON"만 반환하라.

            {
              "place": "가게명",
              "date": "yyyy-MM-dd",
              "total": 0,
              "items": [
                { "name": "상품명", "price": 0, "count": 1 }
              ]
            }

            OCR TEXT:
            %s
            """.formatted(rawText);

            Map<String, Object> inputMessage = Map.of(
                "role", "user",
                "content", List.of(
                    Map.of("type", "input_text", "text", prompt),
                    Map.of(
                        "type", "input_image",
                        "image_url", "data:" + mime + ";base64," + base64
                    )
                )
            );

            Map<String, Object> body = new HashMap<>();
            body.put("model", model);
            body.put("input", List.of(inputMessage));
            body.put("text", Map.of("format", Map.of("type", "json_object")));

            String raw = webClient.post()
                    .uri("/responses")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofMillis(timeoutMs))
                    .onErrorResume(e -> Mono.empty())
                    .block();

            if (raw == null || raw.isBlank()) {
                return null;
            }

            JSONObject resp = new JSONObject(raw);
            String outputJson = extractOutputTextFromResponsesApi(resp);

            if (outputJson == null || outputJson.isBlank()) {
                return null;
            }

            return OpenAiReceiptResult.fromVisionJson(new JSONObject(outputJson));

        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 🔹 [추가된 최소 메서드]
     * ReceiptServiceImp에서 "보조 호출"용
     * - DTO를 건드리지 않음
     * - 단순 위임
     */
    public OpenAiReceiptResult assistIfNeeded(
            MultipartFile file,
            String rawText
    ) {
        if (file == null) {
            return null;
        }
        return fixMissingFieldsWithVision(file, rawText == null ? "" : rawText);
    }

    /**
     * Responses API에서 output_text만 안전하게 추출
     */
    private String extractOutputTextFromResponsesApi(JSONObject resp) {
        if (resp == null) return null;

        JSONArray output = resp.optJSONArray("output");
        if (output == null) return null;

        for (int i = 0; i < output.length(); i++) {
            JSONObject msg = output.optJSONObject(i);
            if (msg == null) continue;

            JSONArray content = msg.optJSONArray("content");
            if (content == null) continue;

            for (int j = 0; j < content.length(); j++) {
                JSONObject c = content.optJSONObject(j);
                if (c == null) continue;

                if ("output_text".equals(c.optString("type"))) {
                    String t = c.optString("text", null);
                    if (t != null && !t.isBlank()) return t;
                }
            }
        }
        return null;
    }
}
