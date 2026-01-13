package com.routerecipt.project.stream;

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
import com.routerecipt.project.service.ReceiptOcrProcessService;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class OcrStreamConsumer implements Runnable {

    private static final String CONSUMER_NAME = "ocr-consumer-1";

    private final RedisTemplate<String, String> redisTemplate;
    private final ReceiptOcrProcessService receiptOcrProcessService;

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
     * Consumer 시작 (단일 인스턴스)
     * =============================== */
    @PostConstruct
    public void start() {
        worker = new Thread(this, "ocr-stream-consumer");
        worker.setDaemon(true);
        worker.start();

        log.info(
            "[OCR-STREAM] Consumer started. group={}, consumer={}",
            RedisStreamConfig.OCR_GROUP,
            CONSUMER_NAME
        );
    }

    /* ===============================
     * Poll Loop
     * =============================== */
    @Override
    public void run() {
        while (running && !Thread.currentThread().isInterrupted()) {
            pollOnce();
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
                    CONSUMER_NAME
                ),
                StreamReadOptions.empty()
                    .block(Duration.ofSeconds(5))
                    .count(1),
                StreamOffset.create(
                    RedisStreamConfig.OCR_STREAM,
                    ReadOffset.lastConsumed()
                )
            );
        } catch (Exception e) {
            log.warn("[OCR-STREAM] Redis polling failed", e);
            sleep(2000);
            return;
        }

        if (records == null || records.isEmpty()) {
            return;
        }

        for (MapRecord<String, Object, Object> record : records) {
            handleAndAck(record);
        }
    }

    /* ===============================
     * Record 처리 + ACK
     * =============================== */
    private void handleAndAck(MapRecord<String, Object, Object> record) {

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

        String receiptNoStr = (String) value.get("receiptNo");
        String imagePath    = (String) value.get("imagePath");

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
        log.info("[OCR-STREAM] Consumer shutting down...");
        running = false;
        if (worker != null) {
            worker.interrupt();
        }
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
