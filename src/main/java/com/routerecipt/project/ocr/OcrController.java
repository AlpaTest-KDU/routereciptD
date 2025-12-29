package com.routerecipt.project.ocr;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.routerecipt.project.stream.OcrStreamProducer;



@RestController
public class OcrController {
	
	 /**
     * OCR 요청 이벤트를 Redis Stream에 발행하는 Producer
     */
	
	private final OcrStreamProducer ocrStreamProducer;
	
	/**
     * 생성자 주입
     */
	
	public OcrController(OcrStreamProducer ocrStreamProducer) {
		this.ocrStreamProducer = ocrStreamProducer;
	}
	
	/**
     * 📌 OCR 요청 API
     *
     * @param receiptId OCR 대상 영수증 ID
     * @param imagePath OCR 대상 이미지 파일 경로
     *
     * 호출 흐름:
     * 1) 클라이언트가 OCR 요청
     * 2) Controller는 요청을 검증
     * 3) Redis Stream에 OCR 이벤트 발행
     * 4) 즉시 응답 반환 (비동기 처리)
     */
	@PostMapping("/ocr/request")
	public String requestOcr(
			@RequestParam("receiptId") String receiptId,
			@RequestParam("imagePath") String imagePath
			) {
		
		// 🔹 OCR 요청 이벤트를 Redis Stream으로 전달
		ocrStreamProducer.publishOcrEvent(receiptId, imagePath);
		
		// 🔹 OCR 처리는 비동기로 진행되므로 즉시 응답
		return "OCR 요청이 정상적으로 접수되었습니다.";
		
	}
	
}
