package com.routerecipt.project.config;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.stream.ReadOffset;
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

    private static final Logger log =
        LoggerFactory.getLogger(RedisStreamConfig.class);

    public static final String OCR_STREAM   = "ocr:receipt";
    public static final String OCR_GROUP    = "ocr-group";

    private final RedisTemplate<String, String> redisTemplate;

    public RedisStreamConfig(
    		@Qualifier("redisTemplate") RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @PostConstruct
    public void initStreamAndGroup() {
        try {
            redisTemplate.opsForStream()
                .createGroup(
                    OCR_STREAM,
                    ReadOffset.from("0-0"),
                    OCR_GROUP
                );

            log.info(
                "[REDIS-STREAM] Group created stream={}, group={}",
                OCR_STREAM,
                OCR_GROUP
            );

        } catch (Exception e) {
            log.warn(
                "[REDIS-STREAM] Group already exists or stream missing",
                e.getMessage()
            );
        }
    }

}

