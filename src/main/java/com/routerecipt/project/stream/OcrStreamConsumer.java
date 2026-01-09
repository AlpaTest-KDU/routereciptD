package com.routerecipt.project.stream;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.routerecipt.project.config.RedisStreamConfig;
import com.routerecipt.project.dto.ReceiptDTO;
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
public class OcrStreamConsumer implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(OcrStreamConsumer.class);

    private final RedisTemplate<String, Object> redisTemplate;
    private final ReceiptApplicationService receiptApplicationService;

    private volatile boolean running = true;
    private Thread worker;

    public OcrStreamConsumer(
            RedisTemplate<String, Object> redisTemplate,
            ReceiptApplicationService receiptApplicationService
    ) {
        this.redisTemplate = redisTemplate;
        this.receiptApplicationService = receiptApplicationService;
    }

    /* ===============================
     * Consumer 시작
     * =============================== */
    @PostConstruct
    public void start() {
        createGroupIfNotExists();

        worker = new Thread(this, "ocr-stream-consumer");
        worker.start();

        log.info("[OCR-STREAM] Consumer started");
    }

    @Override
    public void run() {
        while (running) {
            try {
                pollStream();
            } catch (Exception e) {
                log.error("[OCR-STREAM] polling error", e);
                sleep(2000);
            }
        }
    }

    private void pollStream() {

        List<MapRecord<String, Object, Object>> messages =
                redisTemplate.opsForStream().read(
                        Consumer.from(
                                RedisStreamConfig.OCR_GROUP,
                                RedisStreamConfig.OCR_CONSUMER
                        ),
                        StreamReadOptions.empty()
                                .block(Duration.ofSeconds(5))
                                .count(1),
                        StreamOffset.create(
                                RedisStreamConfig.OCR_STREAM,
                                ReadOffset.lastConsumed()
                        )
                );

        if (messages == null || messages.isEmpty()) {
            return;
        }

        for (MapRecord<String, Object, Object> record : messages) {
            try {
                handleMessage(record);

                redisTemplate.opsForStream().acknowledge(
                        RedisStreamConfig.OCR_STREAM,
                        RedisStreamConfig.OCR_GROUP,
                        record.getId()
                );

            } catch (Exception e) {
                log.error("[OCR-STREAM] message 처리 실패 id={}", record.getId(), e);
            }
        }
    }

    /* ===============================
     * 메시지 처리
     * =============================== */
    private void handleMessage(MapRecord<String, Object, Object> record) {

        Object receiptObj = record.getValue().get("receipt");

        if (!(receiptObj instanceof ReceiptDTO receipt)) {
            log.warn("[OCR-STREAM] invalid receipt payload: {}", record.getValue());
            return;
        }

        log.info("[OCR-STREAM] saving receipt r_no={}", receipt.getR_no());

        // ✅ DB 저장만 수행
        receiptApplicationService.saveReceiptWithItems(receipt);
    }

    /* ===============================
     * Consumer Group 생성
     * =============================== */
    private void createGroupIfNotExists() {
        try {
            // 1️⃣ Stream 존재 보장 (더미 레코드 1개)
            redisTemplate.opsForStream().add(
                    RedisStreamConfig.OCR_STREAM,
                    Map.of("init", "init")
            );

            // 2️⃣ Consumer Group 생성 (가장 안정적인 시그니처)
            redisTemplate.opsForStream().createGroup(
                    RedisStreamConfig.OCR_STREAM,
                    RedisStreamConfig.OCR_GROUP
            );

            log.info("[OCR-STREAM] Consumer group created");

        } catch (Exception e) {
            log.info("[OCR-STREAM] Consumer group already exists");
        }
    }

    @PreDestroy
    public void shutdown() {
        running = false;
        if (worker != null) {
            worker.interrupt();
        }
        log.info("[OCR-STREAM] Consumer stopped");
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {
        }
    }
}
