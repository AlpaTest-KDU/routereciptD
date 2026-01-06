package com.routerecipt.project.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;



/**
 * AI 카테고리 분류 요청(Request) DTO
 *
 * 용도:
 *  - OpenAI 기반 카테고리 분류 API 요청 데이터
 *  - 상품명, OCR 결과 텍스트 등을 전달
 *
 * 요청 예시(JSON):
 * {
 *   "text": "콜라 500ml"
 * }
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AiCategoryRequest {
	
	/**
     * 분류 대상 텍스트
     * - 상품명, 영수증 항목명, 문구 등
     * - null/빈 값일 수 있으므로 실제 검증은 Controller/Service에서 수행
     */
	private String text;
}
