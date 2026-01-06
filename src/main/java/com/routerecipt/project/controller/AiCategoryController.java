package com.routerecipt.project.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.routerecipt.project.OpenAI.OpenAiCategoryService;
import com.routerecipt.project.dto.AiCategoryResponse;

import lombok.RequiredArgsConstructor;


/**
 * AI 카테고리 분류 컨트롤러
 *
 * 역할:
 *  - 상품명/문구를 입력받아 AI로 카테고리 분류
 *  - 테스트 및 내부 호출용 REST API
 *
 * Base URL:
 *  - /ai
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/ai", produces = "application/json")
public class AiCategoryController {

    private static final Logger log =
            LoggerFactory.getLogger(AiCategoryController.class);

    /** OpenAI 기반 카테고리 분류 서비스 */
    private final OpenAiCategoryService openAiCategoryService;

    /**
     * AI 카테고리 분류 API
     *
     * 예시:
     *  GET /ai/category?text=콜라
     *
     * @param text 분류할 텍스트 (상품명 등)
     * @return AI 카테고리 분류 결과
     */
    @GetMapping("/category")
    public AiCategoryResponse classify(
    		// required=false로 두고 직접 검증하여
            // 명확한 에러 메시지를 제공
    		@RequestParam(name = "text", required = false) String text   // ✅ 핵심 수정 포인트
    ) {
    	
    	// 입력값 검증
    	if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("text parameter is required");
        }
    	// 요청 로깅 (운영/분석용)
        log.info("AI CATEGORY REQUEST text='{}'", text);
        
        // AI 분류 서비스 호출
        return openAiCategoryService.classifyItem(text);
    }
}
