package com.routerecipt.project.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.routerecipt.project.OpenAI.OpenAiCategoryService;
import com.routerecipt.project.dto.ItemCategory;
import com.routerecipt.project.dto.ReceiptDTO;
import com.routerecipt.project.dto.ReceiptItemDTO;
import com.routerecipt.project.ocr.OcrService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReceiptOcrProcessService {

    private final OcrService ocrService;
    private final ReceiptCommandService receiptCommandService;
    private final ReceiptService receiptService;
    private final OpenAiCategoryService openAiCategoryService;

    /* =====================================================
     * 🔥 OCR 비동기 처리 단일 진입점 (최종본)
     * ===================================================== */
    @Transactional
    public void processOcr(Long receiptNo) {

        ReceiptDTO baseReceipt = receiptService.getReceiptByNo(receiptNo);

        if (baseReceipt == null || baseReceipt.getR_u() == null) {
            log.error("[OCR] INVALID RECEIPT r_no={}", receiptNo);
            receiptCommandService.updateOcrStatus(receiptNo, "FAIL");
            return;
        }

        if (isBlank(baseReceipt.getImagePath())) {
            log.error("[OCR] IMAGE PATH NOT FOUND r_no={}", receiptNo);
            receiptCommandService.updateOcrStatus(receiptNo, "FAIL");
            return;
        }

        try {
            log.info("[OCR] START r_no={}", receiptNo);

            ReceiptDTO receipt =
                ocrService.processReceiptFromImagePath(
                    baseReceipt.getImagePath(),
                    baseReceipt.getR_u()
                );

            if (receipt == null) {
                log.warn("[OCR] RESULT NULL r_no={}", receiptNo);
                receiptCommandService.updateOcrStatus(receiptNo, "FAIL");
                return;
            }

            receipt.setR_no(receiptNo);
            receipt.setR_u(baseReceipt.getR_u());

            /* 1️⃣ RULE 기반 강제 아이템 */
            applyForcedItemsIfNeeded(receipt);

            /* 2️⃣ 아이템 없는 경우 가맹점 단일 아이템 생성 */
            ensureDefaultItem(receipt);

            /* 3️⃣ AI 분류 (RULE 미분류 항목만) */
            applyAiCategory(receipt);

            /* 4️⃣ FALLBACK (최종 안전망) */
            applyCategoryFallback(receipt);

            /* 5️⃣ DB 반영 */
            receiptCommandService.updateReceiptBasic(
                receiptNo,
                receipt.getR_place(),
                receipt.getR_date(),
                receipt.getR_price()
            );

            receiptCommandService.deleteItemsByReceiptNo(receiptNo);
            receiptCommandService.insertReceiptItems(receipt);

            receiptCommandService.updateOcrStatus(receiptNo, "DONE");
            log.info("[OCR] DONE r_no={}", receiptNo);

        } catch (Exception e) {
            receiptCommandService.updateOcrStatus(receiptNo, "FAIL");
            log.error("[OCR] FAIL r_no={}", receiptNo, e);
        }
    }

    /* =====================================================
     * RULE → AI 이후에도 category 없는 경우만 ETC
     * ===================================================== */
    private void applyCategoryFallback(ReceiptDTO receipt) {

        if (receipt.getItems() == null) return;

        for (ReceiptItemDTO item : receipt.getItems()) {
            if (item == null) continue;

            if (isBlank(item.getItem_category())) {
                item.setItem_category(ItemCategory.ETC.name());
                item.setAi_source("FALLBACK");
                item.setAi_confidence(0.0);
            }
        }
    }

    /* =====================================================
     * RULE 정의
     * ===================================================== */
    private static final List<ForcedItemRule> FORCED_ITEM_RULES = List.of(
        new ForcedItemRule(
            List.of("택시", "TAXI"),
            "택시",
            ItemCategory.TRAFFIC.name(),
            List.of("택시")
        ),
        new ForcedItemRule(
            List.of("주차", "파킹", "PARKING"),
            "주차",
            ItemCategory.TRAFFIC.name(),
            List.of("주차", "파킹")
        ),
        new ForcedItemRule(
            List.of("외과", "내과", "의원", "병원", "의학과", "비뇨기과"),
            "진료비",
            ItemCategory.MEDICAL.name(),
            List.of("진료", "처방", "의료")
        )
    );

    private record ForcedItemRule(
        List<String> placeKeywords,
        String itemName,
        String category,
        List<String> dedupKeywords
    ) {}

    private void applyForcedItemsIfNeeded(ReceiptDTO receipt) {

        String place = safe(receipt.getR_place());
        if (place.isEmpty()) return;

        if (receipt.getItems() == null) {
            receipt.setItems(new ArrayList<>());
        }

        String upperPlace = place.toUpperCase();

        for (ForcedItemRule rule : FORCED_ITEM_RULES) {

            boolean matched = rule.placeKeywords().stream()
                .anyMatch(k -> upperPlace.contains(k.toUpperCase()));

            if (!matched) continue;

            boolean exists = receipt.getItems().stream()
                .filter(i -> i != null && i.getItem_name() != null)
                .anyMatch(i ->
                    rule.dedupKeywords().stream()
                        .anyMatch(i.getItem_name()::contains)
                );

            if (exists) continue;

            ReceiptItemDTO item = new ReceiptItemDTO();
            item.setItem_name(rule.itemName());
            item.setItem_category(rule.category());
            item.setAi_source("RULE");
            item.setAi_confidence(1.0);
            item.setItem_price(receipt.getR_price());

            receipt.getItems().add(item);
        }
    }

    /* =====================================================
     * AI 분류 (RULE 미분류 대상만)
     * ===================================================== */
    private void applyAiCategory(ReceiptDTO receipt) {

        if (receipt.getItems() == null) return;

        for (ReceiptItemDTO item : receipt.getItems()) {

            if (item == null) continue;

            if (!isBlank(item.getItem_category())) {
                continue; // RULE 이미 적용됨
            }

            String itemName = safe(item.getItem_name());
            if (itemName.isEmpty()) continue;

            try {
                log.info("[AI] REQUEST item={}", itemName);

                var ai = openAiCategoryService.classifyItem(itemName);

                if (ai == null || isBlank(ai.getCategory())) {
                    log.warn("[AI] NULL RESULT item={}", itemName);
                    continue;
                }

                log.info("[AI] RESULT item={}, category={}, confidence={}",
                    itemName, ai.getCategory(), ai.getConfidence());

                item.setItem_category(ai.getCategory());
                item.setAi_source(ai.getSource());
                item.setAi_confidence(ai.getConfidence());

            } catch (Exception e) {
                log.error("[AI] FAIL item={}", itemName, e);
            }
        }
    }

    /* =====================================================
     * 아이템 없는 경우 기본 아이템 보장
     * ===================================================== */
    private void ensureDefaultItem(ReceiptDTO receipt) {

        if (receipt.getItems() != null && !receipt.getItems().isEmpty()) return;

        ReceiptItemDTO item = new ReceiptItemDTO();
        item.setItem_name(receipt.getR_place());
        item.setItem_price(receipt.getR_price());

        receipt.setItems(new ArrayList<>(List.of(item)));
    }

    /* =====================================================
     * 유틸
     * ===================================================== */
    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private String safe(String s) {
        return s == null ? "" : s.trim();
    }
}


