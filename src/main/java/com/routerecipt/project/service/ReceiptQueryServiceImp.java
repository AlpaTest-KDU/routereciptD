package com.routerecipt.project.service;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.routerecipt.project.dto.ReceiptDTO;
import com.routerecipt.project.dto.ReceiptItemDTO;
import com.routerecipt.project.mapper.ReceiptMapper;

import lombok.RequiredArgsConstructor;

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
     * 카테고리별 메뉴 Map 생성
     * ===================================================== */
    @Override
    public Map<String, List<String>> buildMenuMap(List<ReceiptDTO> receipts) {

        if (receipts == null) {
            return new HashMap<>();
        }

        return receipts.stream()
                .filter(r -> r.getItems() != null)
                .flatMap(r -> r.getItems().stream())
                .filter(i -> i.getItem_category() != null)
                .collect(Collectors.groupingBy(
                        ReceiptItemDTO::getItem_category,
                        Collectors.mapping(
                                ReceiptItemDTO::getItem_name,
                                Collectors.toList()
                        )
                ));
    }

    /* =====================================================
     * 달력 데이터 생성
     * ===================================================== */
    @Override
    public List<Integer> buildCalendar(String yearMonth) {

        YearMonth ym = YearMonth.parse(yearMonth);
        int lastDay = ym.lengthOfMonth();

        List<Integer> calendar = new ArrayList<>();
        for (int i = 1; i <= lastDay; i++) {
            calendar.add(i);
        }
        return calendar;
    }

    /* =====================================================
     * 최근(임시) 영수증 조회 + 아이템 매핑
     * (네가 올린 기존 로직 그대로)
     * ===================================================== */
    @Override
    public List<ReceiptDTO> getRecentReceipts(List<Long> r_no) {

        if (r_no == null || r_no.isEmpty()) {
            return Collections.emptyList();
        }

        // 1) receipt 조회
        List<ReceiptDTO> receipts = receiptMapper.selectTempReceiptsByNos(r_no);

        if (receipts == null || receipts.isEmpty()) {
            return Collections.emptyList();
        }

        // 2) items 일괄 조회
        List<ReceiptItemDTO> items = receiptMapper.selectItemsByReceiptNos(r_no);

        // 3) r_no 기준 grouping (null key 방지)
        Map<Long, List<ReceiptItemDTO>> itemMap;
        if (items == null || items.isEmpty()) {
            itemMap = Collections.emptyMap();
        } else {
            itemMap = items.stream()
                    .filter(Objects::nonNull)
                    .filter(it -> it.getR_no() != null)
                    .collect(Collectors.groupingBy(ReceiptItemDTO::getR_no));
        }

        for (ReceiptDTO r : receipts) {
            r.setItems(itemMap.getOrDefault(r.getR_no(), Collections.emptyList()));
        }

        // 4) 업로드 순서 유지 정렬
        Map<Long, Integer> order = new HashMap<>();
        for (int i = 0; i < r_no.size(); i++) {
            order.put(r_no.get(i), i);
        }

        receipts.sort(
                Comparator.comparingInt(
                        r -> order.getOrDefault(r.getR_no(), Integer.MAX_VALUE)
                )
        );

        return receipts;
    }
}
