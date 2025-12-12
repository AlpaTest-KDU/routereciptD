package com.routerecipt.project.redis.BloomFilter;

import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedisBloomService {
	
	private final StringRedisTemplate redisTemplate;
	
	public RedisBloomService(StringRedisTemplate redisTemplate) {
		this.redisTemplate = redisTemplate;
	}
	
	//Bloom 필터에 값 추가
	public boolean addUserId(String userId) {
		Long result = redisTemplate.execute((RedisConnection connection) ->  
					(Long) connection.execute(
							"BF.ADD", 
							"userIdBloom".getBytes(), 
							userId.getBytes()
					)
				);
		return result != null && result == 1;
	}
	
	// Bloom Filter에 ID 존재 여부 확인
	public boolean existsUserId(String userId) {
		Long result = redisTemplate.execute((RedisConnection connection) -> 
		(Long) connection.execute(
					"BF.EXISTS",
					"userIdBloom".getBytes(),
					userId.getBytes()
					)
		);
		
		return result != null && result == 1;
	}
	
	
}
