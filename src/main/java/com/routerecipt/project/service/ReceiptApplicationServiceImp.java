package com.routerecipt.project.service;


import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import com.routerecipt.project.dto.AiCategoryResponse;
import com.routerecipt.project.dto.ItemCategory;
import com.routerecipt.project.dto.ReceiptDTO;
import com.routerecipt.project.dto.ReceiptItemDTO;
import com.routerecipt.project.dto.Userdto;
import com.routerecipt.project.OpenAI.AiCategoryService;

import com.routerecipt.project.mapper.ReceiptMapper;
import com.routerecipt.project.mapper.UserMapper;
import com.routerecipt.project.service.ReceiptApplicationServiceImp;

import lombok.AllArgsConstructor;

@org.springframework.stereotype.Service
@AllArgsConstructor
public class ReceiptApplicationServiceImp implements ReceiptApplicatoinService {
	
	private static final Logger log = LoggerFactory.getLogger(ReceiptApplicationServiceImp.class);

    private final ReceiptMapper receiptMapper;
    private final AiCategoryService aiCategoryService;

    /* =====================================================
     * 1️⃣ 영수증 + 아이템 + AI 분류 저장
     * ===================================================== */
    @Transactional
    public void saveReceiptWithItems(ReceiptDTO receipt) {

        if (receipt.getItems() != null && !receipt.getItems().isEmpty()) {

            for (ReceiptItemDTO item : receipt.getItems()) {

                String text = item.getItem_name();

                if (text == null || text.isBlank()) {
                    item.setItem_category(ItemCategory.ETC.name());
                    item.setAi_source("FALLBACK");
                    item.setAi_confidence(0.0);
                    continue;
                }

                AiCategoryResponse ai =
                        aiCategoryService.classifyItem(text);

                item.setItem_category(
                        ai != null && ai.getCategory() != null
                                ? ai.getCategory()
                                : ItemCategory.ETC.name()
                );

                item.setAi_source(ai.getSource());
                item.setAi_confidence(ai.getConfidence());
            }
        }

        // 부모 카테고리
        receipt.setCategory(pickReceiptCategory(receipt));

        // 영수증 저장
        receiptMapper.insertReceipt(receipt);
        Long rNo = receipt.getR_no();

        // 아이템 저장
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
     * 2️⃣ 월별 영수증 조회 🔥 (없어서 에러났던 부분)
     * ===================================================== */
    public List<ReceiptDTO> getSavedReceiptsDate(String userId, String yearMonth) {
        return receiptMapper.getSavedReceiptsDate(userId, yearMonth);
    }

    /* =====================================================
     * 3️⃣ 카테고리별 메뉴 Map 생성 🔥
     * ===================================================== */
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
     * 4️⃣ 달력 데이터 생성 🔥
     * ===================================================== */
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
     * 5️⃣ 부모 카테고리 결정
     * ===================================================== */
    private String pickReceiptCategory(ReceiptDTO receipt) {

        if (receipt == null || receipt.getItems() == null) {
            return ItemCategory.ETC.name();
        }

        Map<String, Long> count =
                receipt.getItems().stream()
                        .filter(i -> i.getItem_category() != null)
                        .collect(Collectors.groupingBy(
                                ReceiptItemDTO::getItem_category,
                                Collectors.counting()
                        ));

        return count.entrySet().stream()
                .filter(e -> !ItemCategory.ETC.name().equals(e.getKey()))
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(ItemCategory.ETC.name());
    }
 }
    
   
    
    



    
