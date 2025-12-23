package com.routerecipt.project.dto;

import lombok.Data;

@Data
public class ReceiptItemDTO {
	
	private Long item_id;
	private Long r_no;
	private String item_name;
	private String item_category;
	private int item_price;
	
	// AI 분류 메타데이터
	private String ai_source; // RULE / AI / FALLBACK
	private Double ai_confidence; // 0.0 ~ 1.0
	
}
