package com.routerecipt.project.service;


import java.util.*;
import java.time.YearMonth;
import java.util.function.Function;

import java.util.stream.Collectors;

import com.routerecipt.project.dto.ItemCategory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.routerecipt.project.dto.MyPageSummaryDTO;
import com.routerecipt.project.dto.ReceiptDTO;
import com.routerecipt.project.dto.ReceiptItemDTO;
import com.routerecipt.project.mapper.ReceiptMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReceiptQueryServiceImp implements ReceiptQueryService {

    private final ReceiptMapper receiptMapper;

    /* =====================================================
     * 월별 영수증 조회
     * ===================================================== */
    @Override
    public List<ReceiptDTO> getSavedReceiptsDate(String userId, String yearMonth) {
        return receiptMapper.getSavedReceiptsDate(userId, yearMonth);
    }

    /* =====================================================
     * 최근(임시) 영수증 조회 + 아이템 매핑
     * (네가 올린 기존 로직 그대로)
     * ===================================================== */
    @Override
    public List<ReceiptDTO> getRecentReceipts(List<Long> r_no) {

        // 🔍 0. 진입 로그
        log.info("[QUERY] getRecentReceipts() called, r_no = {}", r_no);

        if (r_no == null || r_no.isEmpty()) {
            log.warn("[QUERY] r_no is null or empty → return empty list");
            return Collections.emptyList();
        }

        // 1️⃣ receipt 조회
        List<ReceiptDTO> receipts = receiptMapper.selectTempReceiptsByNos(r_no);

        log.info("[QUERY] receipt 조회 결과 size = {}",
                receipts == null ? "null" : receipts.size());

        if (receipts == null || receipts.isEmpty()) {
            log.warn("[QUERY] receipt 조회 결과 없음 → return empty list");
            return Collections.emptyList();
        }

        // 2️⃣ items 일괄 조회
        List<ReceiptItemDTO> items =
                receiptMapper.selectItemsByReceiptNos(r_no);

        log.info("[QUERY] item 조회 결과 size = {}",
                items == null ? "null" : items.size());

        // 3️⃣ r_no 기준 grouping
        Map<Long, List<ReceiptItemDTO>> itemMap;
        if (items == null || items.isEmpty()) {
            log.warn("[QUERY] items 비어 있음 → itemMap empty");
            itemMap = Collections.emptyMap();
        } else {
            itemMap = items.stream()
                    .filter(Objects::nonNull)
                    .filter(it -> it.getR_no() != null)
                    .collect(Collectors.groupingBy(ReceiptItemDTO::getR_no));

            log.info("[QUERY] itemMap grouping keys = {}", itemMap.keySet());
        }

        // 4️⃣ receipt에 item 세팅
        for (ReceiptDTO r : receipts) {
            List<ReceiptItemDTO> attached =
                    itemMap.getOrDefault(r.getR_no(), Collections.emptyList());

            r.setItems(attached);

            log.info(
                "[QUERY] receipt r_no={} → items attached size={}",
                r.getR_no(),
                attached.size()
            );
        }

        // 5️⃣ 업로드 순서 유지 정렬
        Map<Long, Integer> order = new HashMap<>();
        for (int i = 0; i < r_no.size(); i++) {
            order.put(r_no.get(i), i);
        }

        receipts.sort(
            Comparator.comparingInt(
                r -> order.getOrDefault(r.getR_no(), Integer.MAX_VALUE)
            )
        );

        log.info("[QUERY] 최종 반환 receipts size = {}", receipts.size());

        return receipts;
    }

    // 마이페이지용 통계 데이터 집계 (영수증 건수, 총 지출, 최다 카테고리)
    @Override
    public MyPageSummaryDTO getMyPageSummary(String userId) {
        // 1. 날짜 생성 (yyyy-MM)
        YearMonth now = YearMonth.now();
        String currentYearMonth = now.toString();
        String oneMonthAgo = now.minusMonths(1).toString();

        // 2. 데이터 조회
        List<ReceiptDTO> receipts = receiptMapper.getSavedReceiptsDate(userId, currentYearMonth);
        List<ReceiptDTO> agoReceipts = receiptMapper.getSavedReceiptsDate(userId, oneMonthAgo);

        List<ReceiptDTO> currentList = (receipts != null) ? receipts : Collections.emptyList();
        List<ReceiptDTO> agoList = (agoReceipts != null) ? agoReceipts : Collections.emptyList();

        // 영수증 건수 확인 및 비교
        int receiptCount = currentList.size();
        int agoReceiptCount = agoList.size();
        int compareCount = receiptCount - agoReceiptCount;

        String countStatus = (compareCount > 0) ? "증가" : (compareCount < 0) ? "감소" : "증감 없음";
        String compareCountText = (compareCount == 0)
                ? countStatus
                : String.format("지난 달 대비 %d건 %s", Math.abs(compareCount), countStatus);

        // 총 지출 금액 계산 (ReceiptDTO의 r_price 합산)
        long totalAmount = currentList.stream().mapToLong(ReceiptDTO::getR_price).sum();

        // 가장 많은 소비액이 있는 날짜
        String topAmountReceipt = currentList.stream()
                .max(Comparator.comparingInt(ReceiptDTO::getR_price))
                .map(r -> String.format("가장 많이 소비한 날짜 : %d월 %d일",
                        r.getR_date().getMonthValue(), r.getR_date().getDayOfMonth()))
                .orElse("이번 달 내역이 없습니다.");

        // 가장 많이 쓴 카테고리 도출
        String topCategory = getTopCategory(currentList);

        return MyPageSummaryDTO.builder()
                .receiptCount(receiptCount)
                .totalAmount(totalAmount)
                .topCategory(topCategory)
                .compareCountText(compareCountText)
                .topAmountReceipt(topAmountReceipt)
                .build();
    }

    // 코드가 너무 길어져서 따로 메소드로 빼서 작성
    private String getTopCategory(List<ReceiptDTO> receipts) {
        return receipts.stream()
                .map(ReceiptDTO::getItems) // List<ReceiptItemDTO> 추출
                .filter(Objects::nonNull) // null 제외
                .flatMap(List::stream)     // Stream<ReceiptItemDTO>로 변환
                .map(ReceiptItemDTO::getItem_category) // 카테고리 추출 (String)
                .filter(Objects::nonNull)
                .map(this::getKoreanName)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting())) // 카테고리별 그룹화 및 counting
                .entrySet().stream()
                .max(Map.Entry.comparingByValue()) // 빈도수가 가장 높은 엔트리 찾기
                .map(Map.Entry::getKey) // 가장 많이 나타난 key값 추출
                .orElse("이번 달 내역이 없습니다.");
    }

    // Enum 변환 후 한글 이름 반환
    private String getKoreanName(String categoryStr) {
        try {
            return ItemCategory.valueOf(categoryStr).getKoreanName();
        } catch (IllegalArgumentException e) {
            return "기타";
        }
    }
    
    @Override
    public List<ReceiptDTO> getRecentReceiptsByUser(String userId, int limit) {

        if (userId == null || limit <= 0) {
            return Collections.emptyList();
        }

        // 1️⃣ 사용자 기준 최근 영수증 조회
        List<ReceiptDTO> receipts =
                receiptMapper.selectRecentReceiptsByUser(userId, limit);

        if (receipts == null || receipts.isEmpty()) {
            return Collections.emptyList();
        }

        // 2️⃣ r_no 목록 추출
        List<Long> receiptNos = receipts.stream()
                .map(ReceiptDTO::getR_no)
                .filter(Objects::nonNull)
                .toList();

        if (receiptNos.isEmpty()) {
            return receipts;
        }

        // 3️⃣ 아이템 일괄 조회
        List<ReceiptItemDTO> items =
                receiptMapper.selectItemsByReceiptNos(receiptNos);

        Map<Long, List<ReceiptItemDTO>> itemMap =
                (items == null || items.isEmpty())
                        ? Collections.emptyMap()
                        : items.stream()
                               .filter(Objects::nonNull)
                               .filter(i -> i.getR_no() != null)
                               .collect(Collectors.groupingBy(ReceiptItemDTO::getR_no));

        // 4️⃣ receipt ↔ items 매핑
        for (ReceiptDTO r : receipts) {
            r.setItems(itemMap.getOrDefault(
                    r.getR_no(),
                    Collections.emptyList()
            ));
        }

        return receipts;
    }

}
