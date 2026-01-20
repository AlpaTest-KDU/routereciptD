package com.routerecipt.project.service;

import java.time.LocalDate;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.routerecipt.project.dto.AiTrainingItemDTO;
import com.routerecipt.project.dto.ReceiptDTO;
import com.routerecipt.project.dto.ReceiptItemDTO;
import com.routerecipt.project.mapper.ReceiptMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReceiptCommandServiceImp implements ReceiptCommandService {

    private static final Logger log =
        LoggerFactory.getLogger(ReceiptCommandServiceImp.class);

    private final ReceiptMapper receiptMapper;
    private final AiTrainingItemService aiTrainingItemService;

    /* =====================================================
     * 🚫 LEGACY (동기 구조 – 현재 OCR에서는 사용 안 함)
     * ===================================================== */
    @Override
    @Transactional
    public void saveReceiptWithItems(ReceiptDTO receipt) {

        if (receipt == null) return;

        if (receipt.getR_date() == null) receipt.setR_date(LocalDate.now());
        if (receipt.getR_price() == null) receipt.setR_price(0);

        receiptMapper.insertReceipt(receipt);

        Long r_no = receipt.getR_no();
        List<ReceiptItemDTO> items = receipt.getItems();

        if (r_no != null && items != null && !items.isEmpty()) {
            for (ReceiptItemDTO it : items) {
                it.setR_no(r_no);

                if (it.getItem_name() == null || it.getItem_name().isBlank()) {
                    log.warn("Skip item with blank name. r_no={}, item={}", r_no, it);
                    continue;
                }

                receiptMapper.insertItem(it);
            }
        }
    }

    /* =====================================================
     * ✅ 사용자 확정 (수기 수정)
     * ===================================================== */
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

        // 1. 영수증 기본 정보 업데이트
        receiptMapper.updateReceiptBasic(r_no, r_place, r_date, r_price);

        // 2. 기존 아이템 삭제
        receiptMapper.deleteItemsByReceiptNo(r_no);

        // 3. 확정 아이템 일괄 저장
        receiptMapper.insertItemsBatch(
            r_no, item_names, item_prices, item_categories
        );

        // 4. 🔥 AI 재학습 데이터 저장 (Confirm 시점)
        for (int i = 0; i < item_names.size(); i++) {

            String itemName     = item_names.get(i);
            String finalLabel   = item_categories.get(i);

            if (itemName == null || itemName.isBlank()) {
                continue;
            }

            AiTrainingItemDTO ai = new AiTrainingItemDTO();
            ai.setItem_text(itemName);

            // ⚠️ predicted_label 은 "OCR/AI 최초 추론값"
            // 지금 구조상 없다면 null 허용 (RULE/ETC 로 분류됨)
            ai.setPredicted_label(null);
            ai.setPredicted_confidence(0.0);
            ai.setAi_source("USER_CONFIRM");
            ai.setModel_version("v1");

            // ⭐ 사용자 확정 값
            ai.setFinal_label(finalLabel);

            aiTrainingItemService.saveTrainingItem(ai);
        }
    }


    /* =====================================================
     * 🔥 OCR 비동기 전용 (핵심)
     * ===================================================== */

    @Override
    public void updateOcrStatus(Long r_no, String status) {
        receiptMapper.updateOcrStatus(r_no, status);
    }

    @Override
    public void updateOcrStatusByImagePath(String imagePath, String status) {
        receiptMapper.updateOcrStatusByImagePath(imagePath, status);
    }

    @Override
    @Transactional
    public void insertPendingReceipt(ReceiptDTO receipt) {

        if (receipt == null) return;

        if (receipt.getR_u() == null || receipt.getImagePath() == null) {
            log.warn("[PENDING] invalid receipt data r_u={}, imagePath={}",
                     receipt.getR_u(), receipt.getImagePath());
            return;
        }

        if (receipt.getOcr_status() == null) {
            receipt.setOcr_status("PENDING");
        }

        receiptMapper.insertPendingReceipt(receipt);

        log.info("[PENDING] receipt inserted r_no={}, imagePath={}",
                 receipt.getR_no(), receipt.getImagePath());
    }

    /* =====================================================
     * 🔥 OCR 결과 DB 반영 (자동)
     * ===================================================== */

    @Override
    public void updateReceiptBasic(
            Long r_no,
            String r_place,
            LocalDate r_date,
            Integer r_price
    ) {
        receiptMapper.updateReceiptBasic(r_no, r_place, r_date, r_price);
    }

    @Override
    public void deleteItemsByReceiptNo(Long r_no) {
        receiptMapper.deleteItemsByReceiptNo(r_no);
    }

    @Override
    @Transactional
    public void insertReceiptItems(ReceiptDTO receipt) {

        if (receipt == null ||
            receipt.getR_no() == null ||
            receipt.getItems() == null ||
            receipt.getItems().isEmpty()) {
            return;
        }

        Long r_no = receipt.getR_no();

        for (ReceiptItemDTO item : receipt.getItems()) {
            if (item == null ||
                item.getItem_name() == null ||
                item.getItem_name().isBlank()) {
                continue;
            }

            item.setR_no(r_no);
            receiptMapper.insertItem(item);
        }
    }
}
