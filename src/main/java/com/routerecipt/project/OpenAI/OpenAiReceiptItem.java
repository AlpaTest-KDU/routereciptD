package com.routerecipt.project.OpenAI;



import java.util.ArrayList;
import java.util.List;

import lombok.Data;


/**
 * OpenAI OCR 보강 결과 중 "아이템 한 줄"을 표현하는 DTO
 *
 * 사용 위치:
 *  - OpenAiReceiptResult.items 내부 요소
 *  - OpenAI Vision + OCR TEXT 보강 결과를 수신하는 용도
 *
 * 예시(JSON):
 * {
 *   "name": "콜라",
 *   "price": 1500,
 *   "count": 1
 * }
 */
@Data
public class OpenAiReceiptItem {
    private String name;	// 상품명(ReceiptItemDTO.item_name과 매칭되는 값)
    private Integer price;  // 상품 가격(OpenAI가 추론하지 못하면 null일 수 있음)
    private Integer count;  // 상품 수량(현재 로직에서는 사용하지 않지만 OpenAI 응답에 포함될 수 있어 확장 대비로 유지)
}