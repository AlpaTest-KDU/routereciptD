package com.routerecipt.project.OpenAI;

import java.util.Base64;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import lombok.RequiredArgsConstructor;


/**
 * OpenAI Vision(Responses API) 기반 영수증 추출 서비스
 *
 * 역할:
 *  - 영수증 이미지(byte[])를 OpenAI 비전 모델에 전달하여
 *    place/date/total/items 형태의 구조화된 JSON을 얻는다.
 *
 * 주의:
 *  - 현재 RestTemplate을 new로 생성하고 있어 RestTemplateConfig의 타임아웃 설정이 적용되지 않음
 *    (실무에서는 Bean 주입 권장)
 *  - Responses API 응답에서 output_text 추출 방식이 실제 응답 구조와 다를 수 있음
 *    (보통 output[].content[] 안의 output_text 타입에서 꺼냄)
 */
@Service
@RequiredArgsConstructor
public class OpenAiVisionReceiptService {
	
	// OpenAI API Key (프로퍼티 키 이름이 openai.api.key 로 되어 있음)
    @Value("${openai.api.key}")
    private String apiKey;

    // 필요하면 application.properties에서 변경 가능하게 빼도 됨
    private final String model = "gpt-4.1-mini"; // 비전 입력 예시가 문서에 있는 모델 :contentReference[oaicite:0]{index=0}
    
    // OpenAI 호출용 RestTemplate (현재는 new로 생성)
    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 이미지(영수증)를 OpenAI 비전으로 읽어서
     * {place, date(yyyy-MM-dd), total, items:[{name, price, count}]} 형태 JSON 반환
     */
    public JSONObject extractReceiptFromImage(byte[] imageBytes, String contentType) {
    	
    	// 1) contentType을 안전하게 정규화 (기본 jpeg)
        String mime = normalizeMime(contentType); // image/jpeg, image/png
        
        // 2) 바이트 -> base64 -> data URL 생성
        String base64 = Base64.getEncoder().encodeToString(imageBytes);
        String dataUrl = "data:" + mime + ";base64," + base64; // data URL 방식 :contentReference[oaicite:1]{index=1}

        // 3) 프롬프트: "반드시 JSON만" 출력하도록 강제
        String prompt = """
        당신은 영수증 파서입니다.
        이미지의 텍스트를 읽고 아래 JSON 스키마로만 출력하세요. 다른 문장 금지.

        출력 JSON:
        {
          "place": "가게명(없으면 null)",
          "date": "yyyy-MM-dd (없으면 null)",
          "total": 0,
          "items": [
            { "name": "상품명", "price": 0, "count": 1 }
          ]
        }

        규칙:
        - date는 영수증에 적힌 결제일자를 우선 (예: 2021-09-28).
        - total은 '총합계/받을금액/합계' 등을 우선.
        - items는 표의 품목 라인을 최대한 추출.
        - 숫자는 콤마 제거 후 정수로.
        - 불확실하면 null/0/빈배열로.
        """;

        // 4) Responses API 요청 바디 구성 (input_text + input_image)
        JSONObject body = new JSONObject();
        body.put("model", model);

        JSONArray input = new JSONArray();
        JSONObject userMsg = new JSONObject();
        userMsg.put("role", "user");

        JSONArray content = new JSONArray();
        content.put(new JSONObject().put("type", "input_text").put("text", prompt));
        content.put(new JSONObject().put("type", "input_image").put("image_url", dataUrl)); // :contentReference[oaicite:2]{index=2}
        userMsg.put("content", content);

        input.put(userMsg);
        body.put("input", input);

        // 5) HTTP 요청 헤더 구성
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        HttpEntity<String> req = new HttpEntity<>(body.toString(), headers);
        
        // 6) OpenAI /v1/responses 호출
        ResponseEntity<String> resp = restTemplate.exchange(
                "https://api.openai.com/v1/responses",
                HttpMethod.POST,
                req,
                String.class
        );
        
        // 7) 응답 상태/바디 체크
        if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
            throw new IllegalStateException("OpenAI 응답 실패: " + resp.getStatusCode());
        }

        // 8) 모델 출력(JSON 문자열) 추출
        JSONObject root = new JSONObject(resp.getBody());
        String outputText = root.optString("output_text", "").trim();
        if (outputText.isEmpty()) {
            throw new IllegalStateException("OpenAI output_text 비어있음. raw=" + resp.getBody());
        }

        // 9) 혹시 ```json ... ``` 코드펜스가 섞였을 때 제거
        outputText = stripCodeFence(outputText);
        
        // 10) 최종 JSON 파싱해서 반환
        return new JSONObject(outputText);
    }
    
    /**
     * contentType을 이미지 MIME 타입으로 정규화
     * - 미지정이면 image/jpeg
     */
    private String normalizeMime(String ct) {
        if (ct == null) return "image/jpeg";
        ct = ct.toLowerCase();
        if (ct.contains("png")) return "image/png";
        if (ct.contains("webp")) return "image/webp";
        return "image/jpeg";
    }
    
    
    /**
     * ``` 또는 ```json 같은 코드펜스가 포함된 경우 제거
     * - 모델이 프롬프트를 어겼을 때 대비
     */
    private String stripCodeFence(String s) {
        String t = s.trim();
        if (t.startsWith("```")) {
            t = t.replaceFirst("^```[a-zA-Z]*\\s*", "");
            t = t.replaceFirst("\\s*```$", "");
        }
        return t.trim();
    }
}
