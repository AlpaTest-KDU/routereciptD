package com.routerecipt.project.redis.BloomFilter;

import org.springframework.stereotype.Service;

@Service
public class RedisBloomService {

    private static final String USER_BLOOM_KEY = "user:duplicate";

    private final BloomFilterHelper bloomFilterHelper;

    public RedisBloomService(BloomFilterHelper bloomFilterHelper) {
        this.bloomFilterHelper = bloomFilterHelper;
    }

    /** 이미 존재하는 값인지 확인 */
    public boolean isDuplicate(String value) {
        return bloomFilterHelper.exists(USER_BLOOM_KEY, value);
    }

    /** 값 등록 */
    public void register(String value) {
        bloomFilterHelper.add(USER_BLOOM_KEY, value);
    }
}
