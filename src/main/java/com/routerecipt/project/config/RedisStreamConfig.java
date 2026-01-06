package com.routerecipt.project.config;

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
	
	/** OCR 요청을 적재할 Redis Stream 이름 */
	public static final String OCR_STREAM = "ocr:receipt";
	
	/** OCR Stream을 소비하는 Consumer Group 이름 */
	public static final String OCR_GROUP = "ocr-group";
	
	/** Redis Stream 제어를 위한 RedisTemplate */
	private final RedisTemplate<String, String> redisTemplate;
	
	/**
     * 생성자 주입
     *
     * @param redisTemplate Redis Stream 연산에 사용할 RedisTemplate
     */
	public RedisStreamConfig(RedisTemplate<String, String> redisTemplate) {
		this.redisTemplate = redisTemplate;
	}
	
	/**
     * 애플리케이션 기동 시 1회 실행
     *
     * - Redis Stream에 Consumer Group 생성
     * - 이미 존재하는 경우 예외 발생 → 무시 (중복 생성 방지)
     */
	@PostConstruct
	public void initStreamAndGroup() {
		try {
			redisTemplate.opsForStream()
				// Stream 이름
	            // 시작 오프셋: 0-0 (Stream의 처음부터)
	            // Consumer Group 이름
				.createGroup(OCR_STREAM, ReadOffset.from("0-0"),OCR_GROUP);
		} catch (Exception e) {
			// 이미 Consumer Group이 존재하는 경우 등
            // 애플리케이션 기동을 막지 않기 위해 예외 무시
			
		}
	}
}
