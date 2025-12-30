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

@Service
@RequiredArgsConstructor
public class RankingService {
    private final StringRedisTemplate redisTemplate;
    private final ReceiptMapper receiptMapper;
    
    private static final String SPENDING_RANKING_KEY = "user:spending:ranking";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");

    private String getTotalKey(LocalDate receiptTime) {
        return String.format("%s:total:%s", SPENDING_RANKING_KEY, receiptTime.format(DATE_FORMATTER));
    }

    private String getUserKey(String userId, LocalDate receiptTime) {
        return String.format("%s:%s:%s", SPENDING_RANKING_KEY, userId, receiptTime.format(DATE_FORMATTER));
    }

    // 영수증 내역 추가 또는 업데이트
    public void recordSpending(String category, double amount, String userId, LocalDate receiptTime) {
        LocalDate checkTime = (receiptTime != null) ? receiptTime : LocalDate.now();

        redisTemplate.opsForZSet().incrementScore(getTotalKey(checkTime), category, amount);
        redisTemplate.opsForZSet().incrementScore(getUserKey(userId, checkTime), category, amount);

        redisTemplate.expire(getTotalKey(checkTime), 360, TimeUnit.DAYS);
        redisTemplate.expire(getUserKey(userId, checkTime), 360, TimeUnit.DAYS);
    }

    // 카테고리별 랭킹 조회
    // 전체 인원일 경우 userId가 ""
    public Set<ZSetOperations.TypedTuple<String>> getCategoryRanking(int limit, LocalDate receiptTime,
                                                                 String userId) {
        String key = getTotalKey(receiptTime);
        if (userId != null && !userId.isEmpty()) {
            key = getUserKey(userId, receiptTime);
        }

        Set<ZSetOperations.TypedTuple<String>> ranking = redisTemplate.opsForZSet().
                                                         reverseRangeWithScores(key, 0, limit - 1);

        // Redis에 데이터가 없으면 DB에서 복구 시도
        if (ranking == null || ranking.isEmpty()) {
            restoreRankingFromDB(userId, receiptTime);
            ranking = redisTemplate.opsForZSet().reverseRangeWithScores(key, 0, limit - 1);
        }

        return ranking;
    }

    private void restoreRankingFromDB(String userId, LocalDate receiptTime) {
        // todo
        // 전체인원 랭킹 복구에 경우 데이터량이 많을 수 있으서 잠시 보류
        if (userId == null || userId.isEmpty()) {
            return;
        }

        // DB 조회를 위한 날짜 포맷 (예: 2023-10)
        String yearMonth = receiptTime.format(DATE_FORMATTER);
        List<ReceiptDTO> receipts = receiptMapper.getSavedReceiptsDate(userId, yearMonth);

        if (receipts != null && !receipts.isEmpty()) {
            String key = getUserKey(userId, receiptTime);
            for (ReceiptDTO r : receipts) {
                if (r.getItems() != null) {
                    for (ReceiptItemDTO item : r.getItems()) {
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