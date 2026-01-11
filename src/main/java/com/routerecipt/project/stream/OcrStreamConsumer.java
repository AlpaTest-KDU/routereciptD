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

    	List<MapRecord<String, String, String>> records =
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


        if (records == null || records.isEmpty()) return;

        for (MapRecord<String, String, String> record : records) {
            try {
                handle(record);
                streamOps().acknowledge(
                    RedisStreamConfig.OCR_STREAM,
                    RedisStreamConfig.OCR_GROUP,
                    record.getId()
                );
            } catch (Exception e) {
                log.error("[OCR-STREAM] HANDLE FAIL id={}", record.getId(), e);
            }
        }
    }

    private void handle(MapRecord<String, String, String> record) {

        Map<String, String> value = record.getValue();

        String receiptNoStr = value.get("receiptNo");
        String imagePath = value.get("imagePath");

        if (receiptNoStr == null || imagePath == null) {
            log.error("[OCR-STREAM] INVALID PAYLOAD {}", value);
            return;
        }

        Long receiptNo;
        try {
            receiptNo = Long.valueOf(receiptNoStr);
        } catch (NumberFormatException e) {
            log.error("[OCR-STREAM] INVALID receiptNo {}", receiptNoStr);
            return;
        }

        log.info("[OCR-STREAM] PROCESS r_no={}, imagePath={}", receiptNo, imagePath);

        // ✅ 단일 OCR 진입점
        receiptOcrProcessService.processOcr(receiptNo, imagePath);
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
