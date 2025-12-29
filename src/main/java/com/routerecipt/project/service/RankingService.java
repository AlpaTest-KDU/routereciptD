package com.routerecipt.project.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RankingService {
    private final RedisTemplate<String, String> redisTemplate;
    private static final String SPENDING_RANKING_KEY = "user:spending:ranking";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");

    private String getTotalKey(LocalDateTime receiptTime) {
        return String.format("%s:total:%s", SPENDING_RANKING_KEY, receiptTime.format(DATE_FORMATTER));
    }

    private String getUserKey(String userId, LocalDateTime receiptTime) {
        return String.format("%s:%s:%s", SPENDING_RANKING_KEY, userId, receiptTime.format(DATE_FORMATTER));
    }

    // 영수증 내역 추가 또는 업데이트
    public void recordSpending(String category, double amount, String userId, LocalDateTime receiptTime) {
        LocalDateTime checkTime = (receiptTime != null) ? receiptTime : LocalDateTime.now();

        redisTemplate.opsForZSet().incrementScore(getTotalKey(checkTime), category, amount);
        redisTemplate.opsForZSet().incrementScore(getUserKey(userId, checkTime), category, amount);

        redisTemplate.expire(getTotalKey(checkTime), 360, TimeUnit.DAYS);
        redisTemplate.expire(getUserKey(userId, checkTime), 360, TimeUnit.DAYS);
    }

    // 카테고리별 랭킹 조회
    // 전체 인원일 경우 userId가 ""
    public Set<ZSetOperations.TypedTuple<String>> getUserRanking(int limit, LocalDateTime receiptTime,
                                                                 String userId) {
        String key = getTotalKey(receiptTime);
        if (!userId.isEmpty()) {
            key = getUserKey(userId, receiptTime);
        }

        Set<ZSetOperations.TypedTuple<String>> ranking = redisTemplate.opsForZSet().
                                                         reverseRangeWithScores(key, 0, limit - 1);

//        todo(일정 기간이 지나 redis에서 랭킹이 제거되었을 때 다시 불러오는 기능 만들어야 함)
//        if (ranking == null || ranking.isEmpty()) {
//            ranking = fetchRankingFormDB(userId, receiptTime);
//            updateRedisFromDB(key, ranking);
//        }

        return ranking;
    }

    // 전체 데이터 초기화
    public void resetTotalRanking(LocalDateTime receiptTime) {
        redisTemplate.delete(getTotalKey(receiptTime));
    }

    // 사용자 데이터 초기화
    public void resetUserRanking(String userId, LocalDateTime receiptTime) {
        redisTemplate.delete(getUserKey(userId, receiptTime));
    }
}