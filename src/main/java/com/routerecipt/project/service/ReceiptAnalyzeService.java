package com.routerecipt.project.service;

import org.springframework.web.multipart.MultipartFile;

public interface ReceiptAnalyzeService {
	
	Long analyzeReceipt(MultipartFile file, String userId);
}
