package com.routerecipt.project.redis.BloomFilter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

/**
 * RedisBloom 설정 클래스
 *
 * 역할:
 *  - RedisBloom(Cuckoo Filter)를 사용하는 BloomFilterHelper를
 *    Spring Bean으로 등록
 *
 * 특징:
 *  - Redis 접속 정보는 spring.data.redis.* 설정을 그대로 사용
 *  - 애플리케이션 전체에서 하나의 BloomFilterHelper 인스턴스 공유
 */
@Configuration
public class RedisBloomConfig {
	
	// BloomFilterHelper Bean 등록
	@Bean
	@Lazy
    public BloomFilterHelper bloomFilterHelper(
            @Value("${spring.data.redis.host}") String host,
            @Value("${spring.data.redis.port}") int port
    ) {
		// RedisBloom Helper 생성
        return new BloomFilterHelper(host, port);
    }
}
