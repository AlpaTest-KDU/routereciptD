package com.routerecipt.project.redis.BloomFilter;


import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.protocol.CommandArgs;

/**
 * RedisBloom(Cuckoo Filter) Helper 클래스
 *
 * 역할:
 *  - RedisBloom 모듈의 CF.ADD / CF.EXISTS 명령을
 *    Lettuce 저수준 API로 직접 실행
 *
 * 사용 목적:
 *  - 중복 처리 방지 (OCR, 이벤트, 업로드 등)
 */
@SuppressWarnings({"unchecked", "rawtypes"}) // 경고 여기서만 숨김
public class BloomFilterHelper {
	private final io.lettuce.core.RedisClient client;
	
	// Redis 연결 (byte[] 기반)
    private final StatefulRedisConnection<byte[], byte[]> conn;
    
    // BloomFilterHelper 생성자
    public BloomFilterHelper(String host, int port) {

        String url = "redis://" + host + ":" + port;
        System.out.println("🔥 [Bloom] Redis URL = " + url);

        try {
        	// RedisClient 생성
            this.client = RedisClient.create(url);
            System.out.println("🔥 [Bloom] RedisClient 생성 완료");
            
            // byte[] 기반 연결 (RedisBloom 명령에 안전)
            this.conn = client.connect(ByteArrayCodec.INSTANCE);
            System.out.println("🔥 [Bloom] Redis 연결 성공!");
        } catch (Exception e) {
            System.out.println("❌ [Bloom] RedisBloom 초기화 실패!!!");
            e.printStackTrace();
            throw e;		// 초기화 실패는 치명적 → 즉시 중단
        }
    }

	    // EXISTS
    	// 📌 BloomFilter에 값 존재 여부 확인
	    public boolean exists(String key, String value) {

	        var sync = conn.sync();

	        Boolean exists = (Boolean) sync.dispatch(
	        	    BloomCommand.EXISTS,
	        	    new io.lettuce.core.output.BooleanOutput(ByteArrayCodec.INSTANCE),
	        	    new CommandArgs<>(ByteArrayCodec.INSTANCE)
	        	        .add(key.getBytes())
	        	        .add(value.getBytes())
	        	);

	        	return exists != null && exists;
	    }

	    // ADD
	    // 📌 BloomFilter에 값 추가
	    public void add(String key, String value) {

	        var sync = conn.sync();

	        Boolean result = (Boolean) sync.dispatch(
	                BloomCommand.ADD,
	                new io.lettuce.core.output.BooleanOutput(ByteArrayCodec.INSTANCE),
	                new CommandArgs<>(ByteArrayCodec.INSTANCE)
	                        .add(key.getBytes())
	                        .add(value.getBytes())
	        );

	        // 필요하면 result 출력 가능
	        System.out.println("Bloom ADD result = " + result);
	    }
}
