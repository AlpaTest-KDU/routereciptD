package com.routerecipt.project.dto;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


/**
 * OCR 비동기 처리용 Stream 이벤트 DTO
 *
 * 역할:
 *  - OCR 작업을 비동기로 처리하기 위해
 *    Redis Stream(또는 메시지 큐)에 발행되는 이벤트 데이터
 *
 * 사용 흐름:
 *  Controller / Service
 *      ↓
 *  Redis Stream (ocr:receipt)
 *      ↓
 *  OCR Consumer
 */
@Getter
@Setter
@NoArgsConstructor
public class OcrStreamEventDTO {
	
	private String receiptId;			// 영수증 고유 식별자
	private String imagePath;			// OCR 대상 이미지 경로
	private LocalDateTime requestTime;	// OCR 요청 시각
	
	
	/**
     * OCR 이벤트 생성자
     *
     * @param receiptId 영수증 ID
     * @param imagePath OCR 대상 이미지 경로
     */
	public OcrStreamEventDTO(String receiptId, String imagePath) {
		this.receiptId = receiptId;
		this.imagePath = imagePath;
		this.requestTime = LocalDateTime.now(); 
	}
}
