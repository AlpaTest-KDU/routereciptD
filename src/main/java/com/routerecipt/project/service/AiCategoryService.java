package com.routerecipt.project.service;

import org.springframework.stereotype.Service;

import com.routerecipt.project.OpenAI.OpenAiCategoryService;
import com.routerecipt.project.dto.AiCategoryResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AiCategoryService {
	
	private final OpenAiCategoryService openAiCategoryService;
	
	public AiCategoryResponse classify(String text) {
		return openAiCategoryService.classifyItem(text);
	}
	
}
