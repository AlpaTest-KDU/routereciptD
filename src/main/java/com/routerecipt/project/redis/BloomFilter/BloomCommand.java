package com.routerecipt.project.redis.BloomFilter;

import io.lettuce.core.protocol.ProtocolKeyword;


/**
 * RedisBloom(Cuckoo Filter) 명령어 정의 Enum
 *
 * 역할:
 *  - Lettuce 클라이언트에서 RedisBloom 모듈 명령을
 *    직접 실행하기 위해 사용하는 커스텀 ProtocolKeyword
 *
 * 사용 예:
 *  - CF.ADD    : 필터에 값 추가
 *  - CF.EXISTS : 필터에 값 존재 여부 확인
 */
public enum BloomCommand implements ProtocolKeyword {
	
	
	ADD("CF.ADD"),EXISTS("CF.EXISTS");
	
	// Redis로 전송될 명령어 바이트
	private final byte[] bytes;
	
	// RedisBloom 명령 문자열을 byte[]로 변환
	BloomCommand(String keyword) {
		this.bytes = keyword.getBytes();
	}
	
	// Lettuce가 Redis 서버로 명령을 보낼 때 호출
	@Override
	public byte[] getBytes() {
		return bytes;
	}
}
