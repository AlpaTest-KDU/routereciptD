package com.routerecipt.project.stream;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.routerecipt.project.config.RedisStreamConfig;
import com.routerecipt.project.dto.ReceiptDTO;
import com.routerecipt.project.ocr.OcrService;
import com.routerecipt.project.service.ReceiptApplicationService;

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

    @PostConstruct
    public void startConsumer() {
        createGroupIfNotExists();
        new Thread(this::pollStream, "ocr-stream-consumer").start();
    }

    private void createGroupIfNotExists() {
        try {
            redisTemplate.opsForStream().createGroup(
                    RedisStreamConfig.OCR_STREAM,
                    RedisStreamConfig.OCR_GROUP
            );
        } catch (Exception e) {
            // already exists
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
                e.printStackTrace();
            }
        }
    }

    private void processMessage(MapRecord<String, Object, Object> record) {

        Map<Object, Object> value = record.getValue();

        String userId = (String) value.get("userId");
        String imagePath = (String) value.get("imagePath");

        try {
            // 1️⃣ imagePath → byte[]
            byte[] bytes = Files.readAllBytes(Paths.get(imagePath));

            // 2️⃣ OCR 수행
            ReceiptDTO receipt =
                    ocrService.parseReceiptFromBytes(
                            bytes,
                            Paths.get(imagePath).getFileName().toString()
                    );


            if (receipt == null) return;


            receipt.setR_u(userId);

            // 3️⃣ 모든 정책은 ApplicationService로 위임
            receiptApplicationService.saveReceiptWithItems(receipt);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @PreDestroy
    public void shutdown() {
        running = false;
    }
}
