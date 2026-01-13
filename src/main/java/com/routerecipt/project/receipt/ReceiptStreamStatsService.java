package com.routerecipt.project.receipt;

import java.time.YearMonth;

import org.springframework.data.domain.Range;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.routerecipt.project.util.StreamTimeUtil;


/**
 * Redis Stream 기반 OCR 업로드 통계 서비스
 *
 * 역할:
 *  - Redis Stream(ocr:receipt)에 저장된 이벤트를 기준으로
 *    월별 업로드 건수 및 전월 대비 증감을 계산한다.
 *
 * 특징:
 *  - Stream ID 자체가 시간 정보를 포함하므로
 *    날짜 컬럼 없이도 범위 조회로 집계 가능
 */
@Service
public class ReceiptStreamStatsService {
	
	// RedisTemplate (Stream 조회용)
	private final RedisTemplate<String, String> redisTemplate;
	
	// OCR 이벤트가 저장되는 Redis Stream 키 
	private static final String STREAM_KEY = "ocr:receipt";
	
	public ReceiptStreamStatsService(RedisTemplate<String, String> redisTemplate) {
		this.redisTemplate = redisTemplate;
	}
	
	
     // 📌 특정 월 업로드 건수 (Redis Stream ID 기준)
	public long countThisMonth() {
		YearMonth now = YearMonth.now();
		return countByMonth(now);
	}
	
	// 📌 이번 달과 지난달 업로드 건수 차이
	public long diffFromLastMonth() {
		YearMonth thisMonth = YearMonth.now();
		YearMonth lastMonth = thisMonth.minusMonths(1);
		
		return countByMonth(thisMonth) - countByMonth(lastMonth);
	}
	
	// 📌 특정 연월에 해당하는 OCR 업로드 건수 계산
	private long countByMonth(YearMonth ym) {
		
		Range<String> range = Range.closed(
				StreamTimeUtil.startId(ym),
				StreamTimeUtil.endId(ym)
		);
		
		// Stream 범위 조회 후 엔트리 개수 반환
		return redisTemplate.opsForStream()
				.range(STREAM_KEY,range)
				.size();
	}
	
	
}
