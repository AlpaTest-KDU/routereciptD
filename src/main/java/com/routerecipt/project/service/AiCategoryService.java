package com.routerecipt.project.service;

import org.springframework.stereotype.Service;

import com.routerecipt.project.OpenAI.OpenAiCategoryService;
import com.routerecipt.project.dto.AiCategoryResponse;

import lombok.RequiredArgsConstructor;


/**
 * AI 카테고리 분류를 담당하는 비즈니스 서비스
 * - 컨트롤러와 OpenAI 연동 로직 사이의 중간 계층
 * - 향후 분류 로직 확장, fallback, 캐싱 등을 처리하기에 적합한 위치
 */
@Service
@RequiredArgsConstructor
public class AiCategoryService {
	
	// OpenAI API와 직접 통신하는 서비스
	private final OpenAiCategoryService openAiCategoryService;
	
	// 텍스트를 기반으로 AI 카테고리 분류 수행
	public AiCategoryResponse classify(String text) {
		
		// 실제 AI 분류 로직은 OpenAI 서비스에 위임
		return openAiCategoryService.classifyItem(text);
	}
	
}
