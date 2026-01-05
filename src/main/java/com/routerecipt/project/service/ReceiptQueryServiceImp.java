package com.routerecipt.project.service;

import java.util.*;
import java.time.YearMonth;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.routerecipt.project.dto.MyPageSummaryDTO;
import com.routerecipt.project.dto.ReceiptDTO;
import com.routerecipt.project.dto.ReceiptItemDTO;
import com.routerecipt.project.mapper.ReceiptMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReceiptQueryServiceImp implements ReceiptQueryService {

    private final ReceiptMapper receiptMapper;

    @Override
    public List<ReceiptDTO> getRecentReceipts(List<Long> receiptNos) {

        if (receiptNos == null || receiptNos.isEmpty()) {
            return Collections.emptyList();
        }

        // 1) receipt 조회
        List<ReceiptDTO> receipts =
                receiptMapper.selectTempReceiptsByNos(receiptNos);

        if (receipts == null || receipts.isEmpty()) {
            return Collections.emptyList();
        }

        // 2) items 일괄 조회
        List<ReceiptItemDTO> items =
                receiptMapper.selectItemsByReceiptNos(receiptNos);

     // 3) r_no 기준 grouping  ✅ (여기 블록을 교체)
        Map<Long, List<ReceiptItemDTO>> itemMap;
        if (items == null || items.isEmpty()) {
            itemMap = Collections.emptyMap();
        } else {
            itemMap = items.stream()
                    .filter(Objects::nonNull)          // item 자체 null 방지
                    .filter(it -> it.getR_no() != null) // ✅ groupingBy 키 null 방지 (NPE 원인)
                    .collect(Collectors.groupingBy(ReceiptItemDTO::getR_no));
        }

        for (ReceiptDTO r : receipts) {
            r.setItems(itemMap.getOrDefault(r.getR_no(), Collections.emptyList()));
        }

        // 4) 업로드 순서 유지 정렬
        Map<Long, Integer> order = new HashMap<>();
        for (int i = 0; i < receiptNos.size(); i++) {
            order.put(receiptNos.get(i), i);
        }

        receipts.sort(
            Comparator.comparingInt(
                r -> order.getOrDefault(r.getR_no(), Integer.MAX_VALUE)
            )
        );

        return receipts;
    }

    // 마이페이지용 통계 데이터 집계 (영수증 건수, 총 지출, 최다 카테고리)
    @Override
    public MyPageSummaryDTO getMyPageSummary(String userId) {
        // 1. '이번 달' 기준 날짜 생성 (yyyy-MM)
        String currentYearMonth = YearMonth.now().toString();

        // 2. 이번 달 영수증 및 아이템 조회 (기존 Mapper 메서드 재사용)
        List<ReceiptDTO> receipts = receiptMapper.getSavedReceiptsDate(userId, currentYearMonth);

        if (receipts == null || receipts.isEmpty()) {
            return MyPageSummaryDTO.builder()
                    .receiptCount(0)
                    .totalAmount(0L)
                    .topCategory("없음")
                    .build();
        }

        // 3. 영수증 건수
        int receiptCount = receipts.size();

        // 4. 총 지출 금액 계산 (ReceiptDTO의 r_price 합산)
        long totalAmount = receipts.stream()
                .mapToLong(ReceiptDTO::getR_price).sum();

        // 5. 가장 많이 쓴 카테고리 도출
        String topCategory = receipts.stream()
                .map(ReceiptDTO::getItems) // List<ReceiptItemDTO> 추출
                .filter(Objects::nonNull)
                .flatMap(List::stream)     // Stream<ReceiptItemDTO>로 변환
                .map(ReceiptItemDTO::getItem_category)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("없음");

        return MyPageSummaryDTO.builder()
                .receiptCount(receiptCount)
                .totalAmount(totalAmount)
                .topCategory(topCategory)
                .build();
    }
}
