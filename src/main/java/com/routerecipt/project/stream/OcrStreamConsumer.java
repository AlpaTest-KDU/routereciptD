package com.routerecipt.project.stream;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.routerecipt.project.dto.ReceiptDTO;
import com.routerecipt.project.common.Gender;
import com.routerecipt.project.config.RedisStreamConfig;
import com.routerecipt.project.mapper.ReceiptMapper;

/**
 * 📌 OcrStreamConsumer
 *
 * - 역할:
 *   Redis Stream(ocr:receipt)에서 OCR 요청 이벤트를 소비(Consumer)
 *
 * - 특징:
 *   1) 애플리케이션 시작 시 자동으로 실행
 *   2) Consumer Group 기반 처리
 *   3) 메시지를 읽고 OCR 처리 후 ACK 수행
 *
 * 👉 무거운 OCR 작업은 반드시 Consumer에서 처리해야 한다
 */

@Service
public class OcrStreamConsumer {
	/**
     * Redis Stream과 통신하기 위한 Template
     */
	
	@Autowired
	private RedisTemplate<String, Object> redisTemplate;
	
	@Autowired
	private ReceiptMapper receiptMapper;
	
	/**
     * 생성자 주입
     */
	
	public OcrStreamConsumer(RedisTemplate<String, Object> redisTemplate, ReceiptMapper receiptMapper) {
		this.redisTemplate = redisTemplate;
		this.receiptMapper = receiptMapper;
	}
	
	  /**
     * 📌 애플리케이션 시작 시 Consumer Thread 실행
     *
     * - @PostConstruct:
     *   Bean 초기화 완료 후 자동 실행
     * - 별도의 Thread로 Stream을 polling
     */
	@SuppressWarnings("unchecked")
	private void pollStream() {
		
		while (true) {
			try {
				// Consumer Group 정보
				Consumer consumer = Consumer.from(RedisStreamConfig.OCR_GROUP, "ocr-consumer-1");
				
				 // Stream에서 메시지 읽기
				List<MapRecord<String, Object, Object>> messages =
						redisTemplate.opsForStream().read(
								consumer,
								StreamReadOptions.empty()
									.count(5) // 한 번에 처리할 메시지 수
									.block(Duration.ofSeconds(2)), //최대 대기 시간
								StreamOffset.create(
										RedisStreamConfig.OCR_STREAM,
										ReadOffset.lastConsumed() // 마지막 소비 이후부터
										)
								);
				
				// 메시지가 없으면 다음 루프로
				if (messages == null || messages.isEmpty()) {
					continue;
				}
				
				// 메시지 처리
				for (MapRecord<String, Object, Object> record : messages) {
					processMessage(record);
					
					// 정상 처리된 메시지는 ACK
					redisTemplate.opsForStream().acknowledge(
								RedisStreamConfig.OCR_STREAM,
								RedisStreamConfig.OCR_GROUP,
								record.getId()
							);
				}
				
			} catch (Exception e) {
				// consumer 장새 시 로그 출력 후 재시도
				e.printStackTrace();
			}
		}
	}
	/**
     * 📌 단일 OCR 요청 메시지 처리
     *
     * @param record Redis Stream 메시지
     */
	private void processMessage(MapRecord<String, Object, Object> recode) {
		
		// Stream 메시지 데이터 추출
		Map<Object, Object> value = recode.getValue();
		
		String receiptId = (String) value.get("receiptId");
		String imagePath = (String) value.get("imagePath");
		
		/*
         * 🔹 실제 OCR 처리 위치
         *
         * - Clova OCR API 호출
         * - Tesseract 실행
         * - AI 서버 호출 등
         */
		
		String storeName = performOcr(imagePath);
		int totalPrice = 12000;
		
		// 2️⃣ 카테고리 분류 (룰 기반)
        String category = classifyCategory(storeName);

        // 3️⃣ ReceiptDTO 생성
        ReceiptDTO receipt = new ReceiptDTO();
        receipt.setR_u(receiptId);
        receipt.setR_place(storeName);
        receipt.setR_price(totalPrice);
        receipt.setR_date(LocalDate.now());
        receipt.setCategory(category);
        receipt.setGender(Gender.MALE);

		
		/*
         * 🔹 OCR 결과 후처리
         *
         * - OCR 결과 파싱
         * - ReceiptDTO 변환
         * - DB 저장 (Mapper 호출)
         */
		
	}
	
	  /**
     * 📌 실제 OCR 처리 메서드 (현재는 더미 구현)
     *
     * @param imagePath OCR 대상 이미지 경로
     * @return OCR 결과
     */
	
	   private String performOcr(String imagePath) {
	        // TODO: 실제 OCR API 연동
	        return "GS25 강남점";
	    }

	
	private String classifyCategory(String storeName) {

        if (storeName.contains("GS")
                || storeName.contains("CU")
                || storeName.contains("세븐")) {
            return "식비";
        }

        return "기타";
    }
	
}
