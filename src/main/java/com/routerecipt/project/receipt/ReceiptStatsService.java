package com.routerecipt.project.receipt;

import java.time.YearMonth;

import org.springframework.data.domain.Range;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.routerecipt.project.util.StreamTimeUtil;

@Service
public class ReceiptStatsService {
	
	private final RedisTemplate<String, Object> redisTemplate;
	private static final String STREAM_KEY = "ocr:receipt";
	
	public ReceiptStatsService(RedisTemplate<String, Object> redisTemplate) {
		this.redisTemplate = redisTemplate;
	}
	
	/**
     * 📌 특정 월 업로드 건수 (Redis Stream ID 기준)
     */
	
	public long countThisMonth() {
		YearMonth now = YearMonth.now();
		return countByMonth(now);
	}
	
	public long diffFromLastMonth() {
		YearMonth thisMonth = YearMonth.now();
		YearMonth lastMonth = thisMonth.minusMonths(1);
		
		return countByMonth(thisMonth) - countByMonth(lastMonth);
	}
	
	private long countByMonth(YearMonth ym) {
		
		Range<String> range = Range.closed(
				StreamTimeUtil.startId(ym),
				StreamTimeUtil.endId(ym)
		);
		
		return redisTemplate.opsForStream()
				.range(STREAM_KEY,range)
				.size();
	}
	
	
}
