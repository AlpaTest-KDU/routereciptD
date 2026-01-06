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


/**
 * OpenAI 소비 카테고리 분류 서비스
 *
 * 역할:
 *  - FastAPI 서버(OpenAI 모델 연동)에 아이템명을 전달
 *  - 아이템을 소비 카테고리(FOOD, TRAFFIC, ETC 등)로 분류
 *  - 실패 시 안전한 FALLBACK 결과 반환
 */
@Service
@RequiredArgsConstructor
public class OpenAiCategoryService {

    private static final Logger log =
            LoggerFactory.getLogger(OpenAiCategoryService.class);

    // HTTP 통신용 RestTemplate (타임아웃 설정된 Bean 주입 권장)
    private final RestTemplate restTemplate;
    
    // FastAPI 서버 기본 URL 
    @Value("${AI_CATEGORY_URL}")
    private String fastApiBaseUrl;

    /**
     * 단일 아이템 카테고리 분류
     *
     * 처리 흐름:
     *  1) 아이템명을 AiCategoryRequest로 래핑
     *  2) FastAPI /predict 엔드포인트에 POST 요청
     *  3) 응답 JSON을 AiCategoryResponse로 변환
     *  4) 실패 시 FALLBACK(ETC) 반환
     *
     * @param itemName 분류할 아이템명 (예: "콜라", "택시")
     * @return 카테고리 분류 결과
     */
    public AiCategoryResponse classifyItem(String itemName) {
        try {
        	// 요청 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            // 요청 바디 구성
            AiCategoryRequest requestBody = new AiCategoryRequest();
            requestBody.setText(itemName);

            HttpEntity<AiCategoryRequest> request =
                    new HttpEntity<>(requestBody, headers);
            
            // FastAPI 엔드포인트
            String url = fastApiBaseUrl + "/predict";
            
            // POST 호출
            ResponseEntity<String> response =
                    restTemplate.postForEntity(url, request, String.class);
            
            // 디버깅/로그용: 원본 응답
            log.error("RAW AI RESPONSE = {}", response.getBody());
            
            // JSON → 객체 매핑
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(response.getBody(), AiCategoryResponse.class);

        } catch (Exception e) {
        	// 네트워크 오류, 파싱 오류, AI 서버 오류 등
            log.error("AI CALL FAILED", e);
            return fallback();
        }
    }
    
    /**
     * AI 분류 실패 시 반환되는 기본 결과
     *
     * @return category=ETC, confidence=0.0, source=FALLBACK
     */
    private AiCategoryResponse fallback() {
        AiCategoryResponse r = new AiCategoryResponse();
        r.setCategory("ETC");
        r.setConfidence(0.0);
        r.setSource("FALLBACK");
        return r;
    }
}
