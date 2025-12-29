package com.routerecipt.project.service;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.routerecipt.project.dto.ItemCategory;
import com.routerecipt.project.dto.ReceiptDTO;
import com.routerecipt.project.dto.ReceiptItemDTO;

@Service
public class ReceiptCategoryServiceImp implements ReceiptCategoryService {
	
	@Override
	public String pickCategory(ReceiptDTO receipt) {
		 if (receipt == null || receipt.getItems() == null || receipt.getItems().isEmpty()) {
	            return ItemCategory.ETC.name();
	        }

	        Map<String, Long> countByCategory =
	                receipt.getItems().stream()
	                        .filter(i -> i != null && i.getItem_category() != null)
	                        .collect(Collectors.groupingBy(
	                                ReceiptItemDTO::getItem_category,
	                                Collectors.counting()
	                        ));

	        if (countByCategory.isEmpty()) {
	            return ItemCategory.ETC.name();
	        }

	        // ETC 제외 최빈값 우선
	        Optional<Map.Entry<String, Long>> bestNonEtc =
	                countByCategory.entrySet().stream()
	                        .filter(e -> !ItemCategory.ETC.name().equalsIgnoreCase(e.getKey()))
	                        .max(Map.Entry.comparingByValue());

	        if (bestNonEtc.isPresent()) {
	            return bestNonEtc.get().getKey();
	        }

	        // 전부 ETC인 경우
	        return countByCategory.entrySet().stream()
	                .max(Map.Entry.comparingByValue())
	                .map(Map.Entry::getKey)
	                .orElse(ItemCategory.ETC.name());
	    }
}

