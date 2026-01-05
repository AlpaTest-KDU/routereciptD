package com.routerecipt.project.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.routerecipt.project.OpenAI.OpenAiCategoryService;
import com.routerecipt.project.dto.AiCategoryResponse;
import com.routerecipt.project.service.AiCategoryService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/ai", produces = "application/json")
public class AiCategoryController {
	
	private final OpenAiCategoryService openAiCategoryService;
	
	@GetMapping("/category")
	public AiCategoryResponse classify(@RequestParam String text) {
		System.out.println("🔥 CONTROLLER INPUT = [" + text + "]");
		return openAiCategoryService.classifyItem(text);
	}
	
}
