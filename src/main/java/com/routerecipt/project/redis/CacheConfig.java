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

@Configuration
@EnableCaching
public class CacheConfig {
	@Bean
	public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
		
		//JSON + LocalDateTime 직렬화 지원
		ObjectMapper objectMapper = new ObjectMapper()
				.registerModule(new JavaTimeModule())
				.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
				.setSerializationInclusion(JsonInclude.Include.NON_NULL);
		
		GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(objectMapper);
		
		// 기본 TTL : 10분
		RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
				.entryTtl(Duration.ofMinutes(10))
				.serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer))
				.prefixCacheNameWith("cache");
		
		// 캐시 이름별 TTL 커스터마이징
		Map<String, RedisCacheConfiguration> cacheConfigMap = new HashMap<>();
		
		cacheConfigMap.put("userCache", defaultConfig.entryTtl(Duration.ofMinutes(30)));
		cacheConfigMap.put("boardCache", defaultConfig.entryTtl(Duration.ofMinutes(3)));
		
		return RedisCacheManager.builder(connectionFactory)
				.cacheDefaults(defaultConfig)
				.withInitialCacheConfigurations(cacheConfigMap)
				.build();
		
	}
}
