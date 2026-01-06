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


/**
 * OpenAI OCR 보조(Assist) 서비스
 *
 * 역할:
 *  - CLOVA OCR 파싱 결과에서 누락된 필드(상호/날짜/총액/아이템)를 보강하기 위해
 *    OpenAI Vision + OCR TEXT를 함께 사용하여 추론
 *
 * 핵심 특징:
 *  - OpenAI Responses API(/responses) 호출
 *  - 이미지(base64) + OCR 텍스트를 함께 전달
 *  - 응답을 "json_object" 형식으로 강제하여 순수 JSON만 받도록 유도
 */
@Service
public class OpenAiOcrAssisService {
	
	// OpenAI 호출용 WebClient (init()에서 초기화)
    private WebClient webClient;
    
    // OpenAI API Key
    @Value("${openai.api-key}")
    private String apiKey;
    
    // OpenAI API base endpoint (기본값: https://api.openai.com/v1)
    @Value("${openai.endpoint:https://api.openai.com/v1}")
    private String endpoint;

    // 사용할 모델 (기본값: gpt-4.1-mini)
    @Value("${openai.model:gpt-4.1-mini}")
    private String model;
    
    // 네트워크 타임아웃(ms)
    @Value("${openai.timeout-ms:60000}")
    private long timeoutMs;
    
    /**
     * 빈 생성 직후 1회 실행
     * - apiKey 누락 검증
     * - WebClient에 기본 헤더(Authorization / Content-Type)와 baseUrl 적용
     */
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
     *
     * 처리 흐름:
     *  1) 이미지(MultipartFile)를 base64로 인코딩하여 input_image로 전달
     *  2) CLOVA 등에서 추출한 OCR 텍스트(rawText)를 input_text로 전달
     *  3) 모델에게 스키마를 강제하고 "순수 JSON"만 반환하도록 지시
     *  4) Responses API 응답에서 output_text만 추출
     *  5) JSON 파싱 후 OpenAiReceiptResult로 변환하여 반환
     *
     * @param file    영수증 이미지 파일
     * @param rawText OCR에서 추출한 원문 텍스트(누락값 추론 힌트)
     * @return 보강된 결과(OpenAiReceiptResult) 또는 실패 시 null
     */
    public OpenAiReceiptResult fixMissingFieldsWithVision(
            MultipartFile file,
            String rawText
    ) {

        try {
        	// 1) 이미지 -> base64 인코딩
            byte[] bytes = file.getBytes();
            String base64 = java.util.Base64.getEncoder().encodeToString(bytes);
            
            // 2) MIME 타입 결정 (없으면 jpeg로 기본 처리)
            String mime = (file.getContentType() != null)
                    ? file.getContentType()
                    : "image/jpeg";
            
            // 3) 모델에게 요구할 스키마(순수 JSON) 프롬프트 구성
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
            
            // 4) Responses API 입력 메시지 구성
            // - content 배열에 input_text + input_image를 함께 넣는 구조
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
            
            // 5) 요청 바디 구성
            // - text.format.type=json_object 로 "JSON만" 반환하도록 강제
            Map<String, Object> body = new HashMap<>();
            body.put("model", model);
            body.put("input", List.of(inputMessage));
            body.put("text", Map.of("format", Map.of("type", "json_object")));
            
            
            // 6) POST /responses 호출
            // - timeout 적용
            // - 오류는 빈 Mono로 치환 후 block() -> raw가 null이면 실패 처리
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
            
            // 7) Responses API 응답(JSON) 파싱 후 output_text만 추출
            JSONObject resp = new JSONObject(raw);
            String outputJson = extractOutputTextFromResponsesApi(resp);

            if (outputJson == null || outputJson.isBlank()) {
                return null;
            }
            
            // 8) 최종 JSON을 도메인 결과로 변환
            return OpenAiReceiptResult.fromVisionJson(new JSONObject(outputJson));

        } catch (Exception e) {
        	// 예외는 상위로 던지지 않고 null 처리(보강 실패로 간주)
            return null;
        }
    }

    /**
     * 보조 호출용 래퍼 메서드
     *
     * 목적:
     *  - 다른 서비스(ReceiptServiceImp 등)에서 "보강이 필요하면 호출"하는 형태로 쓰기 쉽게 제공
     *  - 내부적으로 fixMissingFieldsWithVision에 위임
     *
     * @param file    영수증 이미지
     * @param rawText OCR 텍스트(없으면 "")
     * @return 보강 결과 또는 null
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

    // Responses API 응답에서 output_text만 안전하게 추출
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
                
                // output_text 타입을 찾아 text 값을 반환
                if ("output_text".equals(c.optString("type"))) {
                    String t = c.optString("text", null);
                    if (t != null && !t.isBlank()) return t;
                }
            }
        }
        return null;
    }
}
