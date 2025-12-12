package com.routerecipt.project.redis.CukooFilter;

import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class CuckooFilterRepository {
	
	private final RedisTemplate<String, String> redisTemplate;
	
	public CuckooFilterRepository(RedisTemplate<String, String> redisTemplate) {
		this.redisTemplate = redisTemplate;
	}
	
	// Cuckoo 필터 생성
	public boolean createCuckooFilter(String key, long capacity) {
		Long result = redisTemplate.execute((RedisConnection connection) -> 
			(Long) connection.execute(
					"CF.RESERVE",
					key.getBytes(),
					Long.toString(capacity).getBytes()
					)
				);
		
		return result != null && result == 1;
	}
	
	// 값 추가
	public boolean add(String key, String value) {
		Long result = redisTemplate.execute((RedisConnection connection) -> 
				(Long) connection.execute(
						"CF.ADD",
						key.getBytes(),
						value.getBytes()
						)
			);
		
		return result != null && result ==1;
	}
	
	// 값 존재 여부 확인
	public boolean exists(String key, String value) {
		Long result = redisTemplate.execute((RedisConnection connection) -> 
				(Long) connection.execute(
						"CF.EXISTS",
						key.getBytes(),
						value.getBytes()
						)
				);
		
		return result != null && result == 1;
	}
	
	// 값 삭제
	public boolean delete(String key, String value) {
		Long result = redisTemplate.execute((RedisConnection connection) -> 
				(Long) connection.execute(
						"CF.DEL",
						key.getBytes(),
						value.getBytes()
						)
				);
		
		return result != null && result ==1;
	}
}
