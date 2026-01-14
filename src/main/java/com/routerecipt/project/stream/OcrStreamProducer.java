package com.routerecipt.project.stream;

import java.util.HashMap;
import java.util.Map;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.routerecipt.project.config.RedisStreamConfig;

import lombok.extern.slf4j.Slf4j;

import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.connection.stream.RecordId;

/**
 * 📌 OcrStreamProducer
 *
 * - 역할: OCR 요청 이벤트를 Redis Stream에 발행(Producer)
 * - 특징:
 *   1) Controller / Service에서 호출됨
 *   2) 실제 OCR 처리는 하지 않음
 *   3) "OCR 요청이 들어왔다"는 사실만 Stream에 기록
 *
 * 👉 MVC 흐름과 OCR 처리 로직을 분리하기 위한 핵심 컴포넌트
 */

@Slf4j
@Service
public class OcrStreamProducer {

    private final RedisTemplate<String, Object> redisStreamTemplate;

    public OcrStreamProducer(
        @Qualifier("redisStreamTemplate")
        RedisTemplate<String, Object> redisStreamTemplate
    ) {
        this.redisStreamTemplate = redisStreamTemplate;
    }

    public void publishOcrEvent(String userId, String imagePath, Long receiptNo) {

        Map<String, Object> message = new HashMap<>();
        message.put("userId", userId);
        message.put("imagePath", imagePath);
        message.put("receiptNo", receiptNo);

        RecordId id = redisStreamTemplate.opsForStream().add(
            StreamRecords.mapBacked(message)
                .withStreamKey(RedisStreamConfig.OCR_STREAM)
        );

        log.info("🔥 [OCR-STREAM] XADD OK id={}, r_no={}", id, receiptNo);
    }
}
