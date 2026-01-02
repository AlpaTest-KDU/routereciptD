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

@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/ai", produces = "application/json")
public class AiCategoryController {

    private static final Logger log =
            LoggerFactory.getLogger(AiCategoryController.class);

    private final OpenAiCategoryService openAiCategoryService;

    /**
     * AI 카테고리 분류 (테스트 / 내부 호출용)
     * GET /ai/category?text=콜라
     */
    @GetMapping("/category")
    public AiCategoryResponse classify(
            @RequestParam(name = "text", required = false) String text   // ✅ 핵심 수정 포인트
    ) {
    	
    	if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("text parameter is required");
        }
    	
        log.info("AI CATEGORY REQUEST text='{}'", text);
        return openAiCategoryService.classifyItem(text);
    }
}
