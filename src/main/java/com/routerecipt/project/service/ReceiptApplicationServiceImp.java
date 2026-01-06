package com.routerecipt.project.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.routerecipt.project.dto.AiCategoryResponse;
import com.routerecipt.project.dto.AiTrainingItemDTO;
import com.routerecipt.project.dto.ReceiptDTO;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReceiptApplicationServiceImp implements ReceiptApplicationService {

    private final ReceiptCommandService receiptCommandService;
    private final ReceiptQueryService receiptQueryService;
    private final AiCategoryService aiCategoryService;
    private final AiTrainingItemService aiTrainingItemService;
    private static final Logger log = LoggerFactory.getLogger(ReceiptApplicationServiceImp.class);

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
            
            log.info("AI RESULT name={}, category={}, source={}",
                    item.getItem_name(),
                    ai.getCategory(),
                    ai.getSource());
            
        });

        // 3️⃣ 영수증 + 아이템 저장
        receiptCommandService.saveReceiptWithItems(receipt);
        
        
    }


    @Override
    @Transactional
    public void confirmReceipt(
            Long r_no,
            String r_place,
            LocalDate r_date,
            Integer r_price,
            List<String> item_names,
            List<Integer> item_prices,
            List<String> item_categories
    ) {
        // 1️⃣ 기존 영수증 / 아이템 확정 처리
        receiptCommandService.confirmReceipt(
                r_no, r_place, r_date, r_price,
                item_names, item_prices, item_categories
        );

        // 2️⃣ 학습 데이터 저장 (여기 추가)
        for (int i = 0; i < item_names.size(); i++) {

            AiTrainingItemDTO ai = new AiTrainingItemDTO();
            ai.setItem_text(item_names.get(i));

            // 현재 구조상 final_label은 item_categories
            ai.setFinal_label(item_categories.get(i));

            // 예측값이 있다면 세팅 (없으면 null 허용)
            ai.setPredicted_label(null);   // or 기존 AI 결과
            ai.setAi_source("AI");
            ai.setModel_version("v1");

            aiTrainingItemService.saveTrainingItem(ai);
        }
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
