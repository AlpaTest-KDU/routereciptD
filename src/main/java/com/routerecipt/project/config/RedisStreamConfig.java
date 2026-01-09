package com.routerecipt.project.config;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

import jakarta.annotation.PostConstruct;

/**
 * Redis Stream 설정 클래스
 *
 * 역할:
 *  - OCR 비동기 처리를 위한 Redis Stream / Consumer Group 정의
 *  - 애플리케이션 기동 시 Stream 및 Consumer Group 자동 생성
 */
@Configuration
public class RedisStreamConfig {

    private static final Logger log = LoggerFactory.getLogger(RedisStreamConfig.class);

    public static final String OCR_STREAM = "ocr:receipt";
    public static final String OCR_GROUP  = "ocr-group";
    public static final String OCR_CONSUMER = "ocr-consumer-1";

    private final RedisTemplate<String, Object> redisTemplate;

    public RedisStreamConfig(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @PostConstruct
    public void initStreamAndGroup() {

        try {
            // 1️⃣ Stream이 없으면 더미 레코드 생성 (필수)
            redisTemplate.opsForStream().add(
                    OCR_STREAM,
                    Map.of("init", "init")
            );

            // 2️⃣ Consumer Group 생성 (Spring Data Redis 3.x 정석)
            redisTemplate.opsForStream()
                    .createGroup(OCR_STREAM, OCR_GROUP);

            log.info("[REDIS-STREAM] Consumer group created: {}", OCR_GROUP);

        } catch (Exception e) {
            log.info("[REDIS-STREAM] Consumer group already exists");
        }
    }
}
