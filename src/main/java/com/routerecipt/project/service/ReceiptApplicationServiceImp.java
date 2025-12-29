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

        /* ===============================
         * 1️⃣ 아이템 단위 AI 분류 + FALLBACK
         * =============================== */
        if (items != null && !items.isEmpty()) {

            for (ReceiptItemDTO item : items) {

                String text = item.getItem_name();

                // 1-1) 상품명이 없으면 무조건 FALLBACK
                if (text == null || text.isBlank()) {
                    item.setItem_category(ItemCategory.ETC.name());
                    item.setAi_source("FALLBACK");
                    item.setAi_confidence(0.0);
                    continue;
                }

                // 1-2) AI 분류 시도
                AiCategoryResponse ai = aiCategoryService.classifyItem(text);

                // 1-3) AI 성공 / 실패 분기
                if (ai != null && ai.getCategory() != null) {
                    item.setItem_category(ai.getCategory());
                    item.setAi_source(ai.getSource());
                    item.setAi_confidence(ai.getConfidence());
                } else {
                    item.setItem_category(ItemCategory.ETC.name());
                    item.setAi_source("FALLBACK");
                    item.setAi_confidence(0.0);
                }
            }
        }

        // 2) 영수증 저장 (부모 category 없음)
        receiptMapper.insertReceipt(receipt);
        Long rNo = receipt.getR_no();

        // 3) 아이템 저장
        if (rNo != null && receipt.getItems() != null) {
            for (ReceiptItemDTO it : receipt.getItems()) {
                it.setR_no(rNo);

                if (it.getItem_category() == null) {
                    it.setItem_category(ItemCategory.ETC.name());
                }
                receiptMapper.insertItem(it);
            }
        }
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
    
   
    
    



    
