package com.routerecipt.project.service;

import com.routerecipt.project.dto.ReceiptDTO;
import com.routerecipt.project.dto.ReceiptItemDTO;
import com.routerecipt.project.mapper.ReceiptMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;


/**
 * 카테고리별 소비 랭킹 서비스
 * - Redis ZSET(정렬된 집합)을 이용해 카테고리별 누적 소비 금액을 score로 관리한다.
 * - 월 단위(yyyyMM)로 전체 랭킹/개인 랭킹을 분리해 저장한다.
 * - Redis에 데이터가 없으면(만료/삭제 등) DB를 조회하여 개인 랭킹을 복구한다.
 */
@Service
@RequiredArgsConstructor
public class RankingService {
	
	// Redis 접근 템플릿(문자열 기반)
    private final StringRedisTemplate redisTemplate;
    
    // DB 복구용 Mapper
    private final ReceiptMapper receiptMapper;
    
    // 랭킹 키 prefix
    private static final String SPENDING_RANKING_KEY = "user:spending:ranking";
    
    // 월 단위 키 생성을 위한 포맷: yyyyMM (예: 202601)
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");
    
    // 전체(모든 사용자) 월별 랭킹 키 생성
    private String getTotalKey(LocalDate receiptTime) {
        return String.format("%s:total:%s", SPENDING_RANKING_KEY, receiptTime.format(DATE_FORMATTER));
    }
    
    // 개인(특정 사용자) 월별 랭킹 키 생성
    private String getUserKey(String userId, LocalDate receiptTime) {
        return String.format("%s:%s:%s", SPENDING_RANKING_KEY, userId, receiptTime.format(DATE_FORMATTER));
    }

    // 영수증 내역이 추가/확정될 때 카테고리별 소비금액을 Redis에 누적 기록
    public void recordSpending(String category, double amount, String userId, LocalDate receiptTime) {
    	
    	// 영수증 날짜가 없으면 오늘로 처리 (월 키 기준 결정)
        LocalDate checkTime = (receiptTime != null) ? receiptTime : LocalDate.now();
        
        // ✅ 전체 랭킹 ZSET에 누적 (member=category, score+=amount)
        redisTemplate.opsForZSet().incrementScore(getTotalKey(checkTime), category, amount);
        
        // ✅ 개인 랭킹 ZSET에도 누적
        redisTemplate.opsForZSet().incrementScore(getUserKey(userId, checkTime), category, amount);
        
        // TTL 설정 (약 1년 보관)
        redisTemplate.expire(getTotalKey(checkTime), 360, TimeUnit.DAYS);
        redisTemplate.expire(getUserKey(userId, checkTime), 360, TimeUnit.DAYS);
    }

    // 카테고리별 랭킹 조회
    // 전체 인원일 경우 userId가 ""
    public Set<ZSetOperations.TypedTuple<String>> getCategoryRanking(int limit, LocalDate receiptTime,
                                                                 String userId) {
    	// 기본은 전체 랭킹 키
        String key = getTotalKey(receiptTime);
        
        // userId가 존재하면 개인 키로 전환
        if (userId != null && !userId.isEmpty()) {
            key = getUserKey(userId, receiptTime);
        }
        
        // score 높은 순으로 상위 limit개 조회
        Set<ZSetOperations.TypedTuple<String>> ranking = redisTemplate.opsForZSet().
                                                         reverseRangeWithScores(key, 0, limit - 1);

        // Redis에 데이터가 없으면 DB 복구 시도 후 재조회
        if (ranking == null || ranking.isEmpty()) {
            restoreRankingFromDB(userId, receiptTime);
            ranking = redisTemplate.opsForZSet().reverseRangeWithScores(key, 0, limit - 1);
        }

        return ranking;
    }
    
    // Redis 랭킹 데이터가 없을 때 DB에서 복구
    private void restoreRankingFromDB(String userId, LocalDate receiptTime) {
        // todo
        // 전체인원 랭킹 복구에 경우 데이터량이 많을 수 있으서 잠시 보류
        if (userId == null || userId.isEmpty()) {
            return;
        }

        // DB 조회를 위한 날짜 포맷 (예: 2023-10)
        String yearMonth = receiptTime.format(DATE_FORMATTER);
        
        // 사용자 + 해당 월 영수증 목록 조회
        List<ReceiptDTO> receipts = receiptMapper.getSavedReceiptsDate(userId, yearMonth);

        if (receipts != null && !receipts.isEmpty()) {
            String key = getUserKey(userId, receiptTime);
            
            // 영수증의 아이템들을 순회하며 카테고리별 금액을 Redis ZSET에 누적
            for (ReceiptDTO r : receipts) {
                if (r.getItems() != null) {
                    for (ReceiptItemDTO item : r.getItems()) {
                    	
                    	// 카테고리가 비어있으면 ETC 처리
                        String category = item.getItem_category();
                        if (category == null || category.isBlank()) {
                            category = "ETC";
                        }
                        // Redis에 점수 누적
                        redisTemplate.opsForZSet().incrementScore(key, category, item.getItem_price());
                    }
                }
            }
            // 복구 후 만료 시간 재설정
            redisTemplate.expire(key, 360, TimeUnit.DAYS);
        }
    }

    // 전체 데이터 초기화
    public void resetTotalRanking(LocalDate receiptTime) {
        redisTemplate.delete(getTotalKey(receiptTime));
    }

    // 사용자 데이터 초기화
    public void resetUserRanking(String userId, LocalDate receiptTime) {
        redisTemplate.delete(getUserKey(userId, receiptTime));
    }
}