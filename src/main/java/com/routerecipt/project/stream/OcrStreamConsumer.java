package com.routerecipt.project.stream;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.core.task.TaskExecutor;
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

@Profile("active")
@Service
@Slf4j
public class OcrStreamConsumer implements Runnable {

    private final RedisTemplate<String, String> redisTemplate;
    private final ReceiptOcrProcessService receiptOcrProcessService;
    private final TaskExecutor taskExecutor;

    private final String consumerName =
            "ocr-consumer-" + UUID.randomUUID();

    private volatile boolean running = true;

    public OcrStreamConsumer(
            RedisTemplate<String, String> redisTemplate,
            ReceiptOcrProcessService receiptOcrProcessService,
            TaskExecutor taskExecutor
    ) {
        this.redisTemplate = redisTemplate;
        this.receiptOcrProcessService = receiptOcrProcessService;
        this.taskExecutor = taskExecutor;
    }

    @PostConstruct
    public void start() {
        taskExecutor.execute(this);
        log.info(
            "[OCR-STREAM] Consumer started. group={}, consumer={}",
            RedisStreamConfig.OCR_GROUP,
            consumerName
        );
    }

    @Override
    public void run() {
        while (running && !Thread.currentThread().isInterrupted()) {
            pollOnce();
        }
    }

    private void pollOnce() {

        List<MapRecord<String, String, String>> records;

        try {
            StreamOperations<String, String, String> streamOps =
                    redisTemplate.opsForStream();

            records = streamOps.read(
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

        } catch (Exception e) {
            log.warn("[OCR-STREAM] Redis polling failed", e);
            sleep(2000);
            return;
        }

        if (records == null || records.isEmpty()) {
            return;
        }

        for (MapRecord<String, String, String> record : records) {
            handleAndAck(record);
        }
    }




    private void handleAndAck(MapRecord<String, String, String> record) {

        try {
            handle(record);

            redisTemplate.opsForStream().acknowledge(
                RedisStreamConfig.OCR_STREAM,
                RedisStreamConfig.OCR_GROUP,
                record.getId()
            );

            log.info("[OCR-STREAM] ACK SUCCESS id={}", record.getId());

        } catch (Exception e) {
            log.error(
                "[OCR-STREAM] HANDLE FAIL id={} value={}",
                record.getId(),
                record.getValue(),
                e
            );
        }
    }

    private void handle(MapRecord<String, String, String> record) {

        Map<String, String> value = record.getValue();
        log.info("[OCR-STREAM] HANDLE value={}", value);

        String receiptNoStr = value.get("receiptNo");
        if (receiptNoStr == null) {
            log.warn("[OCR-STREAM] receiptNo missing value={}", value);
            return;
        }

        Long receiptNo = Long.valueOf(receiptNoStr);

        log.info("[OCR-STREAM] PROCESS r_no={}", receiptNo);

        receiptOcrProcessService.processOcr(receiptNo);
    }

    @PreDestroy
    public void shutdown() {
        running = false;
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
