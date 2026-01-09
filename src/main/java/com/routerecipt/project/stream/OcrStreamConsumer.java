package com.routerecipt.project.stream;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.stereotype.Service;

import com.routerecipt.project.config.RedisStreamConfig;
import com.routerecipt.project.dto.ReceiptDTO;
import com.routerecipt.project.ocr.OcrService;
import com.routerecipt.project.service.ReceiptApplicationService;

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

    private static final Logger log =
            LoggerFactory.getLogger(OcrStreamConsumer.class);

    private final RedisTemplate<String, Object> redisTemplate;
    private final OcrService ocrService;
    private final ReceiptApplicationService receiptApplicationService;

    private volatile boolean running = true;

    public OcrStreamConsumer(
            RedisTemplate<String, Object> redisTemplate,
            OcrService ocrService,
            ReceiptApplicationService receiptApplicationService
    ) {
        this.redisTemplate = redisTemplate;
        this.ocrService = ocrService;
        this.receiptApplicationService = receiptApplicationService;
    }

    /**
     * ✅ Spring Boot 완전 기동 후 Consumer 시작
     */
    @EventListener(ApplicationReadyEvent.class)
    public void startConsumer() {

        log.info("✅ ApplicationReadyEvent 수신 → OCR Stream Consumer 시작");

        createGroupIfNotExists();

        Thread consumerThread =
                new Thread(this::pollStream, "ocr-stream-consumer");
        consumerThread.setDaemon(true);
        consumerThread.start();
    }

    private void createGroupIfNotExists() {
        try {
            redisTemplate.opsForStream().createGroup(
                    RedisStreamConfig.OCR_STREAM,
                    RedisStreamConfig.OCR_GROUP
            );
            log.info("✅ Redis Stream Group 생성 완료");
        } catch (Exception e) {
            log.info("ℹ️ Redis Stream Group 이미 존재");
        }
    }

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

                    redisTemplate.opsForStream().acknowledge(
                            RedisStreamConfig.OCR_STREAM,
                            RedisStreamConfig.OCR_GROUP,
                            record.getId()
                    );
                }

            } catch (Exception e) {
                log.error("❌ Redis Stream 처리 중 오류", e);
                sleep();
            }
        }
    }

    private void processMessage(MapRecord<String, Object, Object> record) {

        Map<Object, Object> value = record.getValue();

        String userId = (String) value.get("userId");
        String imagePath = (String) value.get("imagePath");

        try {
            byte[] bytes = Files.readAllBytes(Paths.get(imagePath));

            ReceiptDTO receipt =
                    ocrService.parseReceiptFromBytes(
                            bytes,
                            Paths.get(imagePath).getFileName().toString()
                    );

            if (receipt == null) return;

            receipt.setR_u(userId);

            receiptApplicationService.saveReceiptWithItems(receipt);

        } catch (Exception e) {
            log.error("❌ OCR 처리 실패: {}", imagePath, e);
        }
    }

    private void sleep() {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException ignored) {}
    }

    @PreDestroy
    public void shutdown() {
        log.info("🔻 OCR Stream Consumer 종료");
        running = false;
    }
}
