package com.routerecipt.project.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.routerecipt.project.OpenAI.OpenAiCategoryService;
import com.routerecipt.project.dto.AiCategoryRequest;
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
