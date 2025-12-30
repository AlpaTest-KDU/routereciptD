package com.routerecipt.project.service;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.routerecipt.project.OpenAI.AiCategoryService;
import com.routerecipt.project.dto.AiCategoryResponse;
import com.routerecipt.project.dto.ItemCategory;
import com.routerecipt.project.dto.ReceiptDTO;
import com.routerecipt.project.dto.ReceiptItemDTO;
import com.routerecipt.project.mapper.ReceiptMapper;

import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@AllArgsConstructor
public class ReceiptApplicationServiceImp implements ReceiptApplicationService {

    private static final Logger log =
            LoggerFactory.getLogger(ReceiptApplicationServiceImp.class);

    private final ReceiptMapper receiptMapper;
    private final AiCategoryService aiCategoryService;

    /* =====================================================
     * 1️⃣ 영수증 + 아이템 + AI 분류 저장
     * ===================================================== */
    @Override
    @Transactional
    public void saveReceiptWithItems(ReceiptDTO receipt) {

        // 0️⃣ 방어 코드
        if (receipt == null) {
            return;
        }

        List<ReceiptItemDTO> items = receipt.getItems();
        String place = receipt.getR_place();

        /* ===============================
         * 1️⃣ 아이템 단위 AI 분류 + FALLBACK
         * =============================== */
        if (items != null && !items.isEmpty()) {

            for (ReceiptItemDTO item : items) {

                String text = item.getItem_name();

                // 1-1) 상품명이 없으면 무조건 FALLBACK
                if (text == null || text.isBlank()) {
                    applyFallback(item);
                    continue;
                }

                // 1-2) AI 분류 시도
                AiCategoryResponse ai = null;
                try {
                    ai = aiCategoryService.classifyItem(text);
                } catch (Exception e) {
                    log.warn("AI classify failed. text={}", text, e);
                }

                // 1-3) AI 성공 / 실패 분기
                if (ai != null && ai.getCategory() != null) {
                    item.setItem_category(ai.getCategory());
                    item.setAi_source(ai.getSource());
                    item.setAi_confidence(ai.getConfidence());
                } else {
                    applyFallback(item);
                }

                // 1-4) ⭐ ETC일 때만 place 기반 "보조 제안"
                if (ItemCategory.ETC.name().equals(item.getItem_category())) {
                    String suggest = suggestByPlace(place);
                    if (suggest != null) {
                        // ❗ 자동 확정 아님 / UI 전달용
                        item.setSuggested_label(suggest);
                    }
                }
            }
        }

        /* ===============================
         * 2️⃣ 영수증 저장
         * =============================== */
        receiptMapper.insertReceipt(receipt);
        Long rNo = receipt.getR_no();

        /* ===============================
         * 3️⃣ 아이템 저장
         * =============================== */
        if (rNo != null && items != null && !items.isEmpty()) {

            for (ReceiptItemDTO it : items) {

                it.setR_no(rNo);

                // 최종 안전장치
                if (it.getItem_category() == null) {
                    applyFallback(it);
                }

                receiptMapper.insertItem(it);
            }
        }
    }
    
    // 분석 후 수정을 하기 위함
   
    public void confirmReceipt(
            Long r_no,
            String r_place,
            LocalDate r_date,
            Integer r_price,
            List<String> item_names,
            List<Integer> item_prices,
            List<String> item_categories
    ) {
        // 1) receipt 기본 정보 업데이트
        receiptMapper.updateReceiptBasic(r_no, r_place, r_date, r_price);

        // 2) 기존 items 삭제
        receiptMapper.deleteItemsByReceiptNo(r_no);

        // 3) 새 items insert
        receiptMapper.insertItemsBatch(r_no, item_names, item_prices, item_categories);
    }

    /* =====================================================
     * place 기반 보조 제안 (자동 확정 ❌)
     * ===================================================== */
    private String suggestByPlace(String place) {
        if (place == null) return null;

        String p = place.replaceAll("\\s+", "").toLowerCase();

        if (p.contains("주차")) {
            return "주차비";
        }
        if (p.contains("택시") || p.contains("카카오t") || p.contains("티맵")) {
            return "택시비";
        }
        if (p.contains("교통카드") || p.contains("티머니") || p.contains("캐시비")) {
            return "교통카드충전";
        }
        return null;
    }

    /* =====================================================
     * FALLBACK 공통 처리
     * ===================================================== */
    private void applyFallback(ReceiptItemDTO item) {
        item.setItem_category(ItemCategory.ETC.name());
        item.setAi_source("FALLBACK");
        item.setAi_confidence(0.0);
    }

    /* =====================================================
     * 2️⃣ 월별 영수증 조회
     * ===================================================== */
    @Override
    public List<ReceiptDTO> getSavedReceiptsDate(String userId, String yearMonth) {
        return receiptMapper.getSavedReceiptsDate(userId, yearMonth);
    }

    /* =====================================================
     * 3️⃣ 카테고리별 메뉴 Map 생성
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
     * 4️⃣ 달력 데이터 생성
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
}
