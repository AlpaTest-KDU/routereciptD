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
import org.springframework.data.redis.core.StreamOperations;
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
    
    private static final String CONSUMER_NAME =
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
                poll();
            } catch (Exception e) {
                log.error("[OCR-STREAM] error", e);
                sleep(2000);
            }
        }
    }

    private StreamOperations<String, String, String> streamOps() {
        return redisTemplate.opsForStream();
    }

    private void poll() {

        List<MapRecord<String, String, String>> records;

        try {
            records =
                streamOps().read(
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
        } catch (org.springframework.data.redis.RedisSystemException e) {
        	
        	if (!running) {
                log.info("[OCR-STREAM] shutdown in progress");
                return;
            }

            Throwable cause = e.getCause();

            if (cause instanceof io.lettuce.core.RedisCommandExecutionException &&
                cause.getMessage().contains("NOGROUP")) {

                log.warn("[OCR-STREAM] Consumer group not ready yet. retry later");
                sleep(3000);
                return;
            }

            throw e; // 진짜 장애만 위로 던짐
        }

        if (records == null || records.isEmpty()) return;

        for (MapRecord<String, String, String> record : records) {
            try {
                boolean handled = handle(record);

                // ✅ 처리 여부와 상관없이 ACK
                streamOps().acknowledge(
                    RedisStreamConfig.OCR_STREAM,
                    RedisStreamConfig.OCR_GROUP,
                    record.getId()
                );

                if (!handled) {
                    log.warn("[OCR-STREAM] ACK invalid payload id={}", record.getId());
                }

            } catch (Exception e) {
                log.error("[OCR-STREAM] HANDLE FAIL id={}", record.getId(), e);
            }
        }
        }

    private boolean handle(MapRecord<String, String, String> record) {

        Map<String, String> value = record.getValue();

        String receiptNoStr = value.get("receiptNo");
        String imagePath = value.get("imagePath");

        if (receiptNoStr == null || imagePath == null) {
            log.warn("[OCR-STREAM] INVALID PAYLOAD {}", value);
            return false; // ❗ invalid지만 ACK 대상
        }

        Long receiptNo;
        try {
            receiptNo = Long.valueOf(receiptNoStr);
        } catch (NumberFormatException e) {
            log.warn("[OCR-STREAM] INVALID receiptNo {}", receiptNoStr);
            return false;
        }

        log.info("[OCR-STREAM] PROCESS r_no={}, imagePath={}", receiptNo, imagePath);
        receiptOcrProcessService.processOcr(receiptNo, imagePath);
        return true;
    }
    private void createGroupIfNotExists() {
        try {
            redisTemplate.opsForStream().createGroup(
                RedisStreamConfig.OCR_STREAM,
                ReadOffset.latest(),
                RedisStreamConfig.OCR_GROUP
            );
        } catch (Exception ignored) {}
    }

    @PreDestroy
    public void shutdown() {
        running = false;
        if (worker != null) worker.interrupt();
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {}
    }
}
