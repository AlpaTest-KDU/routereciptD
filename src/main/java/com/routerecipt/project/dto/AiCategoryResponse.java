package com.routerecipt.project.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


/**
 * AI 카테고리 분류 응답(Response) DTO
 *
 * 용도:
 *  - 상품명/문구에 대한 카테고리 분류 결과 전달
 *  - 분류 결과의 신뢰도와 결정 출처 제공
 *
 * 응답 예시(JSON):
 * {
 *   "category": "DRINK",
 *   "confidence": 0.93,
 *   "source": "AI"
 * }
 */
@Getter
@Setter
@NoArgsConstructor
public class AiCategoryResponse {
	
	/**
     * 분류된 카테고리 값
     * - 보통 Enum 이름 또는 표준 카테고리 코드
     */
	@JsonProperty("category")
	private String category;
	
	/**
     * 분류 신뢰도
     * - 0.0 ~ 1.0 범위
     * - UI에서는 %로 변환 가능
     */
	@JsonProperty("confidence")
	private double confidence;
	
	/**
     * 분류 결과 출처
     * - RULE     : 규칙 기반 분류
     * - AI       : OpenAI 모델 분류
     * - FALLBACK : 기본값/예외 처리 결과
     */
	@JsonProperty("source")
	private String source; 	// RULE / AI / FALLBACK
}
