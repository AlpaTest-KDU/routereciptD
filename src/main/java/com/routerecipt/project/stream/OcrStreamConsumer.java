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

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

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
	
    private final RedisTemplate<String, Object> redisTemplate;
    private final ReceiptMapper receiptMapper;
    private volatile boolean running = true;
    
    public OcrStreamConsumer(
            RedisTemplate<String, Object> redisTemplate,
            ReceiptMapper receiptMapper) {
        this.redisTemplate = redisTemplate;
        this.receiptMapper = receiptMapper;
    }

    /**
     * 📌 애플리케이션 시작 시 Consumer 초기화
     * 1) Consumer Group 생성 보장
     * 2) Consumer Thread 실행
     */
    @PostConstruct
    public void startConsumer() {
        createGroupIfNotExists();
        new Thread(this::pollStream, "ocr-stream-consumer").start();
    }

    /**
     * 📌 Consumer Group 생성 (이미 존재하면 무시)
     */
    private void createGroupIfNotExists() {
        try {
            redisTemplate.opsForStream().createGroup(
                    RedisStreamConfig.OCR_STREAM,
                    RedisStreamConfig.OCR_GROUP
            );
        } catch (Exception e) {
            // 이미 그룹이 존재하는 경우 예외 발생 → 무시
        }
    }

    /**
     * 📌 Redis Stream Polling 루프
     * - 별도 Thread에서 실행
     * - XREADGROUP 기반 소비
     */
    @SuppressWarnings("unchecked")
    private void pollStream() {

        Consumer consumer =
                Consumer.from(RedisStreamConfig.OCR_GROUP, "ocr-consumer-1");

        while (running) {
            try {
                List<MapRecord<String, Object, Object>> messages =
                        redisTemplate.opsForStream().read(
                                consumer,
                                StreamReadOptions.empty()
                                        .count(5)
                                        .block(Duration.ofSeconds(2)),
                                StreamOffset.create(
                                        RedisStreamConfig.OCR_STREAM,
                                        ReadOffset.lastConsumed()
                                )
                        );

                if (messages == null || messages.isEmpty()) {
                    continue;
                }

                for (MapRecord<String, Object, Object> record : messages) {
                    processMessage(record);

                    // ✅ 정상 처리 후 ACK
                    redisTemplate.opsForStream().acknowledge(
                            RedisStreamConfig.OCR_STREAM,
                            RedisStreamConfig.OCR_GROUP,
                            record.getId()
                    );
                }

            } catch (Exception e) {
                // 로그만 남기고 루프 유지 (Consumer는 죽지 않음)
                e.printStackTrace();
            }
        }
    }

    /**
     * 📌 단일 OCR 요청 처리
     */
    private void processMessage(MapRecord<String, Object, Object> record) {

        Map<Object, Object> value = record.getValue();

        String receiptId = (String) value.get("receiptId");
        String imagePath = (String) value.get("imagePath");

        // 1️⃣ OCR 처리
        String storeName = performOcr(imagePath);
        int totalPrice = 12000;

        // 2️⃣ 카테고리 분류
        String category = classifyCategory(storeName);

        // 3️⃣ DTO 생성
        ReceiptDTO receipt = new ReceiptDTO();
        receipt.setR_u(receiptId);
        receipt.setR_place(storeName);
        receipt.setR_price(totalPrice);
        receipt.setR_date(LocalDate.now());
        
        receipt.setGender(Gender.MALE);

        // 4️⃣ DB 저장
        receiptMapper.insertReceipt(receipt);
    }
    
    @PreDestroy
    public void shutdown() {
        running = false;
    }
    
    /**
     * 📌 OCR 더미 메서드
     */
    private String performOcr(String imagePath) {
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
