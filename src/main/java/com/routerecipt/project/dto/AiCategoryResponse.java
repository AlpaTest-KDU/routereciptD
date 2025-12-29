package com.routerecipt.project.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AiCategoryResponse {
	private String category;
	private double confidence;
	private String source; 	// RULE / AI / FALLBACK
}
