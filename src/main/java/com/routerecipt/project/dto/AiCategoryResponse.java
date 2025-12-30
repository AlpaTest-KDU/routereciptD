package com.routerecipt.project.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AiCategoryResponse {
	private String category;
	private double confidence;
	private String source; 	// RULE / AI / FALLBACK
}
