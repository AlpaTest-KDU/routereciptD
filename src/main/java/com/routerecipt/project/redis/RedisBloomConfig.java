package com.routerecipt.project.redis;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedisBloomConfig {
	
	@Bean
	public BloomFilterHelper bloomFilterHelper (
			@Value("${spring.data.redis.host}")	String host,
			@Value("${spring.data.redis.port}") int port
			) {
		return new BloomFilterHelper(host, port);
	}
}
