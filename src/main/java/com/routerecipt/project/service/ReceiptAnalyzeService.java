package com.routerecipt.project.service;

import org.springframework.web.multipart.MultipartFile;


/**
 * 영수증 분석 서비스 인터페이스
 *
 * - 사용자가 업로드한 영수증 파일을 분석하는 유즈케이스의 진입점
 * - OCR, AI 분류, 영수증/아이템 저장 등의 전체 흐름을 추상화한다.
 */
public interface ReceiptAnalyzeService {
	
	// 영수증 파일을 분석하고 결과를 저장한다.
	Long analyzeReceipt(MultipartFile file, String userId);
}
