package com.routerecipt.project.redis.BloomFilter;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;


/**
 * RedisBloom 기반 중복 체크 서비스
 *
 * 역할:
 *  - 특정 값이 이미 처리되었는지 Bloom Filter로 확인
 *  - 새 값을 Bloom Filter에 등록
 *
 * 사용 예:
 *  - 중복 영수증 업로드 방지
 *  - 중복 사용자 이벤트 방지
 *  - 중복 OCR 요청 방지
 */
@Service
public class RedisBloomService {

	// 사용자 중복 체크용 Bloom Filter Key
    private static final String USER_BLOOM_KEY = "user:duplicate";

    // RedisBloom 저수준 Helper
    private final BloomFilterHelper bloomFilterHelper;

    // 생성자 주입
    public RedisBloomService(@Lazy BloomFilterHelper bloomFilterHelper) {
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
