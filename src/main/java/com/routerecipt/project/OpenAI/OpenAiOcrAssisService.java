package com.routerecipt.project.OpenAI;

import java.time.Duration;
import java.util.Base64;
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

    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${openai.endpoint:https://api.openai.com/v1}")
    private String endpoint;

    // Vision 가능한 모델로 고정 권장 (gpt-4o-mini / gpt-4.1-mini 등)
    @Value("${openai.model:gpt-4.1-mini-2025-04-14}")
    private String model;

    @Value("${openai.timeout-ms:60000}") // ✅ 15초는 너무 짧음
    private long timeoutMs;

    @PostConstruct
    public void init() {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("OPENAI_API_KEY 가 비어있습니다.");
        }

        this.webClient = WebClient.builder()
                .baseUrl(endpoint)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey.trim())
                .build();
    }

    /**
     * ✅ B안: 이미지(Vision)로 영수증 핵심 필드 보강
     * @param imageBytes 업로드된 영수증 이미지 bytes
     */
    public OpenAiReceiptResult fixMissingFieldsWithVision(MultipartFile file, String rawText) {
        try {
            // 이미지 -> base64
            byte[] bytes = file.getBytes();
            String b64 = java.util.Base64.getEncoder().encodeToString(bytes);
            String mime = (file.getContentType() != null) ? file.getContentType() : "image/jpeg";

            String prompt = """
            다음은 한국 영수증입니다. 이미지와 OCR 텍스트를 함께 참고해서,
            아래 JSON 스키마로 "순수 JSON"만 반환하세요(설명/코드블록 금지).

            {
              "place": "가게명",
              "date": "yyyy-MM-dd(없으면 빈 문자열)",
              "total": 0,
              "items": [
                { "name": "상품명", "price": 0, "count": 1 }
              ]
            }

            OCR TEXT:
            """ + rawText;

            // ✅ Responses API 형식(텍스트 + 이미지)
            Map<String, Object> inputMessage = Map.of(
                "role", "user",
                "content", List.of(
                    Map.of("type", "input_text", "text", prompt),
                    Map.of("type", "input_image",
                           "image_url", "data:" + mime + ";base64," + b64)
                )
            );

            Map<String, Object> body = new HashMap<>();
            body.put("model", model);
            body.put("input", List.of(inputMessage));
            // JSON만 나오게 강제(가능하면)
            body.put("text", Map.of("format", Map.of("type", "json_object")));

            // ✅ /responses 호출 -> JSONObject로 받기(Wrapper 말고 raw로 받는 게 안전)
            String raw = webClient.post()
                    .uri("/responses")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofMillis(timeoutMs))
                    .onErrorResume(e -> {
                        System.err.println("[VISION] timeout or error → OCR만 사용: " + e.getMessage());
                        return Mono.empty(); // 👈 Vision 실패 시 그냥 스킵
                    })
                    .block();

            if (raw == null || raw.isBlank()) {
                System.err.println("[VISION] 응답 raw 비어있음");
                return null;
            }

            JSONObject resp = new JSONObject(raw);

            // ✅ 너가 만든 함수로 output_text 추출
            String out = extractOutputTextFromResponsesApi(resp);

            if (out == null || out.isBlank()) {
                System.err.println("[VISION] output_text 비어있음. raw=" + resp.toString(2));
                return null;
            }

            // out 자체가 JSON 문자열이므로 파싱
            JSONObject j = new JSONObject(out);
            return OpenAiReceiptResult.fromVisionJson(j);

        } catch (Exception e) {
            System.err.println("[VISION] 호출 실패: " + e.getMessage());
            return null;
        }
    }

    // ✅ 네가 올린 함수: 이 클래스에 private 메서드로 추가하면 됨
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
    
    private OpenAiReceiptResult toResult(JSONObject j) {
        OpenAiReceiptResult r = new OpenAiReceiptResult();
        if (j == null) return r;

        r.setPlace(j.optString("place", null));
        r.setDate(j.optString("date", null));
        r.setTotal(j.has("total") ? j.optInt("total", 0) : 0);

        JSONArray arr = j.optJSONArray("items");
        if (arr != null) {
            for (int i = 0; i < arr.length(); i++) {
                JSONObject it = arr.optJSONObject(i);
                if (it == null) continue;

                OpenAiReceiptResult.Item item = new OpenAiReceiptResult.Item();
                item.setName(it.optString("name", null));
                item.setPrice(it.optInt("price", 0));
                item.setCount(it.optInt("count", 1));

                if (item.getName() != null && !item.getName().isBlank()) {
                    r.getItems().add(item);
                }
            }
        }
        return r;
    }

}
	