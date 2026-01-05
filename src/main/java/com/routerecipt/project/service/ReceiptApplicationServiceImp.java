package com.routerecipt.project.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.routerecipt.project.dto.AiCategoryResponse;
import com.routerecipt.project.dto.ReceiptDTO;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReceiptApplicationServiceImp implements ReceiptApplicationService {

    private final ReceiptCommandService receiptCommandService;
    private final ReceiptQueryService receiptQueryService;
    private final AiCategoryService aiCategoryService;

    /* ===============================
     * Write
     * =============================== */
    @Override
    public void saveReceiptWithItems(ReceiptDTO receipt) {
    	
    	 // 1️⃣ 아이템별 AI 카테고리 분류
        receipt.getItems().forEach(item -> {

            AiCategoryResponse ai =
                    aiCategoryService.classify(item.getItem_name());

            // 2️⃣ 실제 저장 필드에 세팅 (snake_case)
            item.setItem_category(ai.getCategory());
            item.setAi_confidence(ai.getConfidence());
            item.setAi_source(ai.getSource());
        });

        // 3️⃣ 영수증 + 아이템 저장
        receiptCommandService.saveReceiptWithItems(receipt);
    }


    @Override
    public void confirmReceipt(
            Long r_no,
            String r_place,
            LocalDate r_date,
            Integer r_price,
            List<String> item_names,
            List<Integer> item_prices,
            List<String> item_categories
    ) {
        receiptCommandService.confirmReceipt(
                r_no, r_place, r_date, r_price,
                item_names, item_prices, item_categories
        );
    }

    /* ===============================
     * Read
     * =============================== */
    @Override
    public List<ReceiptDTO> getSavedReceiptsDate(String userId, String yearMonth) {
        return receiptQueryService.getSavedReceiptsDate(userId, yearMonth);
    }

    @Override
    public Map<String, List<String>> buildMenuMap(List<ReceiptDTO> receipts) {
        return receiptQueryService.buildMenuMap(receipts);
    }

    @Override
    public List<Integer> buildCalendar(String yearMonth) {
        return receiptQueryService.buildCalendar(yearMonth);
    }

    @Override
    public List<ReceiptDTO> getRecentReceipts(List<Long> r_no) {
        return receiptQueryService.getRecentReceipts(r_no);
    }
}
