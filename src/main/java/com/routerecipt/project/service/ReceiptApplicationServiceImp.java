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


/**
 * 영수증(Receipt) 애플리케이션 서비스 구현체
 *
 * - 화면/요청 단위 유즈케이스를 조립하는 계층
 * - Write(저장/확정)는 CommandService에 위임하고, 필요 시 AI 분류/학습데이터 저장을 함께 수행
 * - Read(조회/달력/메뉴맵/최근 조회)는 QueryService에 위임
 */
@Service
@RequiredArgsConstructor
public class ReceiptApplicationServiceImp implements ReceiptApplicationService {


    private final ReceiptCommandService receiptCommandService;
    private final AiCategoryService aiCategoryService;
    private final AiTrainingItemService aiTrainingItemService;
    private static final Logger log = LoggerFactory.getLogger(ReceiptApplicationServiceImp.class);


    /* ===============================
     * Write
     * =============================== */
    // 영수증 + 아이템 저장
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

    // 영수증 확정 처리
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
}
