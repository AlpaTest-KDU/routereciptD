package com.routerecipt.project.OpenAI;

import java.util.List;

import lombok.Data;

/**
 * OpenAI 카테고리 분류 결과 DTO
 *
 * 역할:
 *  - OpenAI에게 아이템명 리스트를 보내고,
 *    "아이템명 -> 카테고리"로 분류된 결과를 구조화해서 받기 위한 모델
 *
 * 사용 예:
 *  - ReceiptItemDTO.item_name 목록을 OpenAI에 전달
 *  - 응답으로 받은 categories를 순회하며
 *    ReceiptItemDTO.item_category에 매핑
 */
@Data
public class OpenAiCategoryResult {
	
	// 아이템별 카테고리 분류 결과 목록
    private List<ItemCategoryPair> categories;
    
    // 아이템명과 카테고리 분류 결과를 묶는 페어 클래스
    @Data
    public static class ItemCategoryPair {
        private String name;      // 분류 대상 아이템명(item_name과 매칭)
        private String category;  // 분류된 카테고리 코드(FOOD/CLOTHES/MEDICAL/HOME/LIVING/CULTURE/TRAFFIC/ETC)
    }
}
