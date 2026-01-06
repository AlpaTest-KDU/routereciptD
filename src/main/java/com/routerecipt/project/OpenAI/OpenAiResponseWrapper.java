package com.routerecipt.project.OpenAI;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Data;


/**
 * OpenAI Responses API 응답을 감싸는 Wrapper DTO
 *
 * 역할:
 *  - OpenAI Responses API의 복잡한 응답 구조를 그대로 매핑
 *  - 실제 결과(JSON 문자열)를 안전하게 추출
 *  - OpenAiReceiptResult로 변환하는 책임을 담당
 *
 * 사용 위치:
 *  - OpenAI OCR/Vision 응답 파싱 단계
 */
@Data
public class OpenAiResponseWrapper {
	
	// OpenAI Responses API의 output 필드
    private List<Output> output;
    
    // output 요소
    @Data
    public static class Output {
        private List<Content> content;
    }
    
    // content 요소
    @Data
    public static class Content {
        private String text;	// 실제 응답 텍스트
    }
    
    // OpenAI 응답을 OpenAiReceiptResult로 변환
    public OpenAiReceiptResult toResult() {
    	
    	// 1️⃣ output 검증
        if (output == null || output.isEmpty()) {
            throw new IllegalStateException("OpenAI 응답에 output이 없습니다.");
        }

        Output out = output.get(0);
        
        // 2️⃣ content 검증
        if (out.getContent() == null || out.getContent().isEmpty()) {
            throw new IllegalStateException("OpenAI 응답에 content가 없습니다.");
        }
        
        // 3️⃣ text 추출
        String raw = out.getContent().get(0).getText();
        if (raw == null || raw.isBlank()) {
            throw new IllegalStateException("OpenAI 응답 text가 비어있습니다.");
        }

        try {
        	 // 4️⃣ ```json 코드 블록 제거 (OpenAI 응답 특성 대응)
            String json = raw
                    .replaceAll("```json", "")
                    .replaceAll("```", "")
                    .trim();
            
            // 5️⃣ JSON → OpenAiReceiptResult 변환
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(json, OpenAiReceiptResult.class);

        } catch (Exception e) {
        	// JSON 파싱 실패 시 원본 응답을 함께 남김
            throw new IllegalStateException("OpenAI JSON 파싱 실패: " + raw, e);
        }
    }
}
