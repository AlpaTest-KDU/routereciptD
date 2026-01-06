package com.routerecipt.project.service;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.routerecipt.project.dto.ItemCategory;
import com.routerecipt.project.dto.ReceiptDTO;
import com.routerecipt.project.dto.ReceiptItemDTO;

/**
 * 영수증 대표 카테고리를 결정하는 서비스 구현체
 *
 * 규칙:
 * 1) 영수증/아이템이 비어있으면 ETC
 * 2) 아이템의 item_category를 카테고리별로 개수 집계
 * 3) ETC를 제외한 최빈값(가장 많이 나온 카테고리)을 우선 선택
 * 4) 전부 ETC인 경우 ETC 반환
 */
@Service
public class ReceiptCategoryServiceImp implements ReceiptCategoryService {
	
	@Override
	public String pickCategory(ReceiptDTO receipt) {
		// 0) 입력 방어: 영수증/아이템이 없으면 대표 카테고리는 ETC
		 if (receipt == null || receipt.getItems() == null || receipt.getItems().isEmpty()) {
	            return ItemCategory.ETC.name();
	        }
		 
		 	// 1) 카테고리별 "개수" 집계 (null 아이템/카테고리 제외)
	        Map<String, Long> countByCategory =
	                receipt.getItems().stream()
	                        .filter(i -> i != null && i.getItem_category() != null)
	                        .collect(Collectors.groupingBy(
	                                ReceiptItemDTO::getItem_category,
	                                Collectors.counting()
	                        ));
	        // 집계 결과가 없으면 ETC
	        if (countByCategory.isEmpty()) {
	            return ItemCategory.ETC.name();
	        }

	        // 2) ETC를 제외한 카테고리 중에서 최빈값(개수가 가장 큰 값) 선택
	        Optional<Map.Entry<String, Long>> bestNonEtc =
	                countByCategory.entrySet().stream()
	                        .filter(e -> !ItemCategory.ETC.name().equalsIgnoreCase(e.getKey()))
	                        .max(Map.Entry.comparingByValue());
	        
	        // ETC가 아닌 대표 카테고리가 있으면 그것을 반환
	        if (bestNonEtc.isPresent()) {
	            return bestNonEtc.get().getKey();
	        }

	     // 3) 전부 ETC인 경우: ETC(혹은 최빈값) 반환
	        return countByCategory.entrySet().stream()
	                .max(Map.Entry.comparingByValue())
	                .map(Map.Entry::getKey)
	                .orElse(ItemCategory.ETC.name());
	    }
}

