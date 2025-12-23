package com.routerecipt.project.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.core.RedisTemplate;

import jakarta.annotation.PostConstruct;

@Configuration
public class RedisStreamConfig {
	
	// Stream 이름
	public static final String OCR_STREAM = "ocr:receipt";
	
	// Consumer Group 이름
	public static final String OCR_GROUP = "ocr-group";
	
	private final RedisTemplate<String, String> redisTemplate;
	
	public RedisStreamConfig(RedisTemplate<String, String> redisTemplate) {
		this.redisTemplate = redisTemplate;
	}
	
	@PostConstruct
	public void initStreamAndGroup() {
		try {
			redisTemplate.opsForStream()
				.createGroup(OCR_STREAM, ReadOffset.from("0-0"),OCR_GROUP);
		} catch (Exception e) {
			
		}
	}
}
