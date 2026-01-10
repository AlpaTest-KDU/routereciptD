package com.routerecipt.project.stream;

import java.util.HashMap;
import java.util.Map;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.routerecipt.project.config.RedisStreamConfig;


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

@Service
public class OcrStreamProducer {

    private final RedisTemplate<String, String> redisTemplate;

    public OcrStreamProducer(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 📌 OCR 요청 이벤트 발행
     *
     * @param userId    요청 사용자 ID
     * @param imagePath OCR 대상 이미지 경로
     */
    public void publishOcrEvent(String userId, String imagePath) {

        Map<String, String> message = new HashMap<>();
        message.put("userId", userId);
        message.put("imagePath", imagePath);

        redisTemplate.opsForStream()
            .add(RedisStreamConfig.OCR_STREAM, message);
    }
}