package com.routerecipt.project.dto;

import java.util.Date;

import lombok.Data;

/**
 * OCR 영수증 요약 DTO
 *
 * 역할:
 *  - OCR 엔진으로부터 추출한 영수증 핵심 정보 전달
 *  - 상호명, 거래 날짜, 총액만 포함하는 경량 DTO
 *
 * 사용 위치:
 *  - OCR 결과 파싱 단계
 *  - AI 분석/저장 전 중간 데이터 모델
 */
@Data
public class OcrReceiptDTO {
	private String shop;	// 상호명
	private Date date;		// 거래 날짜
	private int total;		// 총 결제 금액
}
