package com.routerecipt.project.config;

import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import lombok.RequiredArgsConstructor;


/**
 * Redis 설정 클래스
 *
 * 역할:
 *  - Redis 서버 연결 설정
 *  - RedisConnectionFactory Bean 등록
 *  - RedisTemplate 직렬화 전략 설정
 */
@Configuration
@RequiredArgsConstructor
public class RedisConfig {
	
	/**
     * Spring Boot가 자동으로 주입하는 Redis 설정 정보
     * (spring.redis.*)
     */
	private final RedisProperties redisProperties;
	
	/**
     * Redis 서버와의 연결을 담당하는 ConnectionFactory
     *
     * @return Lettuce 기반 RedisConnectionFactory
     */
	@Bean
	public RedisConnectionFactory redisConnectionFactory() {
		return new LettuceConnectionFactory(redisProperties.getHost(),redisProperties.getPort());
	}
	
	
	/**
     * Redis 데이터 접근을 위한 RedisTemplate
     *
     * Key   : String (사람이 읽을 수 있는 키)
     * Value : Object (JSON 직렬화)
     */
	@Bean
	@Primary
	public RedisTemplate<String, String> redisTemplate(
	        RedisConnectionFactory connectionFactory) {

	    RedisTemplate<String, String> template = new RedisTemplate<>();
	    template.setConnectionFactory(connectionFactory);

	    StringRedisSerializer stringSerializer = new StringRedisSerializer();

	    template.setKeySerializer(stringSerializer);
	    template.setValueSerializer(stringSerializer);
	    template.setHashKeySerializer(stringSerializer);
	    template.setHashValueSerializer(stringSerializer);

	    return template;
	}


	
	
}
