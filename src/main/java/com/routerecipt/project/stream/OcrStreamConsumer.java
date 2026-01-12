package com.routerecipt.project.stream;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.routerecipt.project.config.RedisStreamConfig;
import com.routerecipt.project.service.ReceiptOcrProcessService;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class OcrStreamConsumer implements Runnable {

    private final RedisTemplate<String, String> redisTemplate;
    private final ReceiptOcrProcessService receiptOcrProcessService;

    private final String consumerName =
            "ocr-consumer-" + UUID.randomUUID();

    private volatile boolean running = true;
    private Thread worker;

    public OcrStreamConsumer(
            RedisTemplate<String, String> redisTemplate,
            ReceiptOcrProcessService receiptOcrProcessService
    ) {
        this.redisTemplate = redisTemplate;
        this.receiptOcrProcessService = receiptOcrProcessService;
    }

    /* ===============================
     * Consumer 시작
     * =============================== */
    @PostConstruct
    public void start() {
        worker = new Thread(this, "ocr-stream-consumer");
        worker.setDaemon(true); // 🔥 JVM 종료 방해 방지
        worker.start();

        log.info(
            "[OCR-STREAM] Consumer started. group={}, consumer={}",
            RedisStreamConfig.OCR_GROUP,
            consumerName
        );
    }

    /* ===============================
     * Poll Loop
     * =============================== */
    @Override
    public void run() {
        while (running) {
            try {
                pollOnce();
            } catch (Exception e) {
                if (!running) {
                    break;
                }
                log.error("[OCR-STREAM] unexpected error", e);
                sleep(3000);
            }
        }

        log.info("[OCR-STREAM] Consumer loop exited");
    }

    /* ===============================
     * 단일 Poll
     * =============================== */
    private void pollOnce() {

        List<MapRecord<String, Object, Object>> records;

        try {
            records = redisTemplate.opsForStream().read(
                Consumer.from(
                    RedisStreamConfig.OCR_GROUP,
                    consumerName
                ),
                StreamReadOptions.empty()
                    .block(Duration.ofSeconds(5))
                    .count(1),
                StreamOffset.create(
                    RedisStreamConfig.OCR_STREAM,
                    ReadOffset.lastConsumed()
                )
            );

        } catch (org.springframework.data.redis.RedisSystemException e) {

            if (!running) {
                return;
            }

            Throwable cause = e.getCause();
            if (cause != null &&
                cause.getMessage() != null &&
                cause.getMessage().contains("NOGROUP")) {

                log.warn("[OCR-STREAM] Consumer group not ready yet. retry...");
                sleep(3000);
                return;
            }

            // 🔥 Redis 종료 / 네트워크 단절 등
            log.warn("[OCR-STREAM] Redis polling failed (ignored)");
            sleep(3000);
            return;
        }

        if (records == null || records.isEmpty()) {
            return;
        }

        for (MapRecord<String, Object, Object> record : records) {
            handleRecordSafely(record);
        }
    }

    /* ===============================
     * Record 처리 + ACK
     * =============================== */
    private void handleRecordSafely(MapRecord<String, Object, Object> record) {

        try {
            boolean handled = handle(record);

            redisTemplate.opsForStream().acknowledge(
                RedisStreamConfig.OCR_STREAM,
                RedisStreamConfig.OCR_GROUP,
                record.getId()
            );

            if (!handled) {
                log.warn(
                    "[OCR-STREAM] ACK invalid payload id={} value={}",
                    record.getId(),
                    record.getValue()
                );
            }

        } catch (Exception e) {
            log.error(
                "[OCR-STREAM] HANDLE FAIL id={} value={}",
                record.getId(),
                record.getValue(),
                e
            );
        }
    }

    /* ===============================
     * 실제 OCR 처리
     * =============================== */
    private boolean handle(MapRecord<String, Object, Object> record) {

        Map<Object, Object> value = record.getValue();

        // 🔥 init 더미 메시지 차단
        if (value.containsKey("init")) {
            log.info("[OCR-STREAM] skip init record {}", record.getId());
            return false;
        }

        String receiptNoStr = (String) value.get("receiptNo");
        String imagePath   = (String) value.get("imagePath");

        if (receiptNoStr == null || imagePath == null) {
            log.warn("[OCR-STREAM] INVALID PAYLOAD {}", value);
            return false;
        }

        Long receiptNo;
        try {
            receiptNo = Long.valueOf(receiptNoStr);
        } catch (NumberFormatException e) {
            log.warn("[OCR-STREAM] INVALID receiptNo {}", receiptNoStr);
            return false;
        }

        log.info(
            "[OCR-STREAM] PROCESS r_no={}, imagePath={}",
            receiptNo,
            imagePath
        );

        receiptOcrProcessService.processOcr(receiptNo, imagePath);
        return true;
    }

    /* ===============================
     * Shutdown
     * =============================== */
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
        } catch (InterruptedException ignored) {}
    }
}
