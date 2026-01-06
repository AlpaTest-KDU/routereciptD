package com.routerecipt.project.redis;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;


/**
 * Redis 기반 Spring Cache 설정 클래스
 *
 * 역할:
 *  - @EnableCaching 으로 스프링 캐시 기능을 활성화
 *  - RedisCacheManager 를 Bean 으로 등록하여
 *    @Cacheable/@CachePut/@CacheEvict 등이 Redis를 사용하도록 설정
 *
 * 특징:
 *  - 값(Value)을 JSON으로 저장
 *  - LocalDateTime 등 Java Time 타입 직렬화를 지원
 *  - 기본 TTL과 캐시 이름별 TTL을 다르게 적용 가능
 */
@Configuration
@EnableCaching
public class CacheConfig {
	
	// RedisCacheManager Bean
	@Bean
	public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
		
		
		// 1) JSON + LocalDateTime 직렬화 지원 ObjectMapper 구성
		ObjectMapper objectMapper = new ObjectMapper()
				.registerModule(new JavaTimeModule())
				.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
				.setSerializationInclusion(JsonInclude.Include.NON_NULL);
		
		// 2) Redis에 저장할 값(Value)을 JSON으로 직렬화하는 Serializer
		GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(objectMapper);
		
		// 3) 기본 캐시 설정 : 10분
		RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
				.entryTtl(Duration.ofMinutes(10))
				.serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer))
				.prefixCacheNameWith("cache");
		
		 // 4) 캐시 이름별 TTL 커스터마이징
		Map<String, RedisCacheConfiguration> cacheConfigMap = new HashMap<>();
		
		// 사용자 관련 캐시는 더 오래 유지 (30분)
		cacheConfigMap.put("userCache", defaultConfig.entryTtl(Duration.ofMinutes(30)));
		
		// 게시판/목록 캐시는 자주 바뀔 수 있으니 짧게 (3분)
		cacheConfigMap.put("boardCache", defaultConfig.entryTtl(Duration.ofMinutes(3)));
		
		// 5) RedisCacheManager 생성
		return RedisCacheManager.builder(connectionFactory)
				.cacheDefaults(defaultConfig)					// 기본 정책
				.withInitialCacheConfigurations(cacheConfigMap)	// 캐시별 정책
				.build();
		
	}
}
