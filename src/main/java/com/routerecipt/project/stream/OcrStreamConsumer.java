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
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.stereotype.Service;

import com.routerecipt.project.config.RedisStreamConfig;
import com.routerecipt.project.dto.ReceiptDTO;
import com.routerecipt.project.ocr.OcrService;
import com.routerecipt.project.service.ReceiptApplicationService;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;

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
@Slf4j
public class OcrStreamConsumer implements Runnable {

    private final RedisTemplate<String, String> redisTemplate;
    private final OcrService ocrService;
    private final ReceiptApplicationService receiptApplicationService;

    private volatile boolean running = true;
    private Thread worker;

    public OcrStreamConsumer(
            RedisTemplate<String, String> redisTemplate,
            OcrService ocrService,
            ReceiptApplicationService receiptApplicationService
    ) {
        this.redisTemplate = redisTemplate;
        this.ocrService = ocrService;
        this.receiptApplicationService = receiptApplicationService;
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
                log.error("[OCR-STREAM] 처리 실패 id={}", record.getId(), e);
            }
        }
    }


    private void handle(MapRecord<String, String, String> record) {

        Map<String, String> value = record.getValue();

        String userId = value.get("userId");
        String imagePath = value.get("imagePath");

        if (userId == null || imagePath == null) {
            log.error("[OCR-STREAM] INVALID PAYLOAD {}", value);
            return;
        }

        log.info("[OCR-STREAM] OCR START userId={}, image={}", userId, imagePath);

        try {
            ReceiptDTO receipt =
                ocrService.processReceiptFromImagePath(imagePath, userId);

            // ❌ OCR 실패 (결과 없음)
            if (receipt == null) {
                receiptApplicationService.updateOcrStatusByImagePath(
                    imagePath, "FAILED"
                );
                log.error("[OCR-STREAM] OCR FAILED image={}", imagePath);
                return;
            }

            // ✅ OCR 성공 → 저장
            receiptApplicationService.saveReceiptWithItems(receipt);

            // ✅ 상태 DONE 확정
            receiptApplicationService.updateOcrStatus(
                receipt.getR_no(), "DONE"
            );

            // ✅ 성공 시에만 temp 파일 삭제
            try {
                Files.deleteIfExists(Paths.get(imagePath));
            } catch (Exception e) {
                log.warn("[OCR-STREAM] TEMP FILE DELETE FAIL path={}", imagePath, e);
            }

            log.info("[OCR-STREAM] OCR DONE r_no={}", receipt.getR_no());

        } catch (Exception e) {
            // ❌ 예외도 OCR 실패로 처리
            receiptApplicationService.updateOcrStatusByImagePath(
                imagePath, "FAILED"
            );
            log.error("[OCR-STREAM] EXCEPTION image={}", imagePath, e);
        }
    }




    private void createGroupIfNotExists() {
        try {
        	redisTemplate.opsForStream().createGroup(
                    RedisStreamConfig.OCR_STREAM,
                    ReadOffset.latest(),
                    RedisStreamConfig.OCR_GROUP
                );
            redisTemplate.opsForStream().createGroup(
                RedisStreamConfig.OCR_STREAM,
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
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }
}

