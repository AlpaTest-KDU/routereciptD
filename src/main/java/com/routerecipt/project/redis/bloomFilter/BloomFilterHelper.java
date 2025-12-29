package com.routerecipt.project.redis.bloomFilter;


import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.protocol.CommandArgs;

@SuppressWarnings({"unchecked", "rawtypes"}) // 경고 여기서만 숨김
public class BloomFilterHelper {
	 private final io.lettuce.core.RedisClient client;
	    private final StatefulRedisConnection<byte[], byte[]> conn;

	    public BloomFilterHelper(String host, int port) {

	        String url = "redis://" + host + ":" + port;
	        System.out.println("🔥 [Bloom] Redis URL = " + url);

	        try {
	            this.client = RedisClient.create(url);
	            System.out.println("🔥 [Bloom] RedisClient 생성 완료");

	            this.conn = client.connect(ByteArrayCodec.INSTANCE);
	            System.out.println("🔥 [Bloom] Redis 연결 성공!");
	        } catch (Exception e) {
	            System.out.println("❌ [Bloom] RedisBloom 초기화 실패!!!");
	            e.printStackTrace();
	            throw e;
	        }
	    }

	    // EXISTS
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
