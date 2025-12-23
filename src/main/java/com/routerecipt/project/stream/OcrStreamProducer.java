package com.routerecipt.project.stream;

import java.util.HashMap;
import java.util.Map;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.routerecipt.project.config.RedisStreamConfig;


/**
 * 📌 OcrStreamProducer
 *
 * - 역할: OCR 요청 이벤트를 Redis Stream에 발행(Producer)
 * - 특징:
 *   1) Controller / Service에서 호출됨
 *   2) 실제 OCR 처리는 하지 않음
 *   3) "OCR 요청이 들어왔다"는 사실만 Stream에 기록
 *
 * 👉 MVC 흐름과 OCR 처리 로직을 분리하기 위한 핵심 컴포넌트
 */

@Service
public class OcrStreamProducer {
	 /**
     * Redis와 통신하기 위한 Template
     * - RedisConfig에서 Bean으로 등록됨
     * - 여기서는 String 기반 Stream 메시지를 사용
     */
	
	private final RedisTemplate<String, String> redisTemplate;
	
	public OcrStreamProducer(RedisTemplate<String, String> redisTemplate) {
		this.redisTemplate = redisTemplate;
	}
	
	 /**
     * 📌 OCR 요청 이벤트를 Redis Stream에 발행
     *
     * @param receiptId OCR 대상 영수증 ID
     * @param imagePath OCR 대상 이미지 경로
     *
     * 동작 설명:
     * 1) receiptId, imagePath를 Map 형태로 구성
     * 2) Redis Stream(ocr:receipt)에 메시지 추가
     * 3) Consumer는 이 메시지를 읽어 OCR 처리 수행
     */
	
	public void publishOcrEvent(String receiptId, String imagePath) {
		
		// stream에 들어갈 메시지 구성 (Key-Value 구조)
		Map<String, String> message = new HashMap<>();
		message.put("receiptId", receiptId);
		message.put("imagePath", imagePath);
		
		  /*
         * Redis Stream에 메시지 발행
         *
         * - Stream 이름: RedisStreamConfig.OCR_STREAM (ocr:receipt)
         * - XADD ocr:receipt * receiptId=xxx imagePath=yyy
         *
         * 반환값:
         * - RecordId (ex: 1700000000000-0)
         * - 지금 단계에서는 사용하지 않음
         */
		
		redisTemplate.opsForStream().add(RedisStreamConfig.OCR_STREAM, message);
	}
}	
