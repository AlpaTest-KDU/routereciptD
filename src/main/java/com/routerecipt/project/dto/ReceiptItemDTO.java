package com.routerecipt.project.dto;

import lombok.Data;


/**
 * 영수증 아이템 DTO
 *
 * 역할:
 *  - 영수증에 포함된 개별 상품(아이템) 1건을 표현
 *  - ReceiptDTO의 자식 객체
 *  - OCR / 수기 입력 / AI 카테고리 분류 결과를 함께 보관
 */
@Data
public class ReceiptItemDTO {
	
	private Long item_id;			// 아이템 고유 ID
	private Long r_no;				// 부모 영수증 ID
	private String item_name;		// 상품명
	private String item_category;	// 상품 카테고리 코드
	private Integer item_price;		// 상품 가격 (원 단위)
	
	// AI 분류 메타데이터
	private String ai_source; // RULE / AI / FALLBACK
	private Double ai_confidence; // AI 분류 신뢰도(0.0 ~ 1.0)
	
	private String suggested_label; // AI 추천 사용자 친화적 라벨
	
}
