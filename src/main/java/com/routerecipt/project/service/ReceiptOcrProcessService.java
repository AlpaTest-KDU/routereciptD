package com.routerecipt.project.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final ReceiptApplicationService receiptApplicationService;
    private final ReceiptCommandService receiptCommandService;
    private final ReceiptService receiptService; // imagePath 조회용

    /* =====================================================
     * 🔥 OCR 비동기 처리 단일 진입점 (유일)
     * ===================================================== */
    @Transactional
    public void processOcr(Long receiptNo) {

        // 1️⃣ DB 기준 imagePath 조회
        String imagePath = receiptService.getImagePathByReceiptNo(receiptNo);

        if (imagePath == null || imagePath.isBlank()) {
            log.error("[OCR] imagePath NOT FOUND r_no={}", receiptNo);
            receiptCommandService.updateOcrStatus(receiptNo, "FAIL");
            return;
        }

        try {
            log.info("[OCR] START r_no={}, imagePath={}", receiptNo, imagePath);

            ReceiptDTO receipt =
                    ocrService.processReceiptFromImagePath(imagePath, null);

            if (receipt == null) {
                log.warn("[OCR] RESULT NULL r_no={}", receiptNo);
                receiptCommandService.updateOcrStatus(receiptNo, "FAIL");
                return;
            }

            // receipt 연결
            receipt.setR_no(receiptNo);

            // RULE
            applyForcedItemsIfNeeded(receipt);

            // FALLBACK
            applyCategoryFallback(receipt);

            // 저장
            receiptApplicationService.saveReceiptWithItems(receipt);

            // 상태 DONE
            receiptCommandService.updateOcrStatus(receiptNo, "DONE");

            log.info("[OCR] DONE r_no={}", receiptNo);

        } catch (Exception e) {
            receiptCommandService.updateOcrStatus(receiptNo, "FAIL");
            log.error("[OCR] FAIL r_no={}", receiptNo, e);

        } finally {
            // temp 파일 정리
            try {
                java.nio.file.Files.deleteIfExists(
                    java.nio.file.Paths.get(imagePath)
                );
            } catch (Exception e) {
                log.warn("[OCR] TEMP FILE DELETE FAIL path={}", imagePath, e);
            }
        }
    }

    /* =====================================================
     * item_category NOT NULL 보장
     * ===================================================== */
    private void applyCategoryFallback(ReceiptDTO receipt) {

        if (receipt.getItems() == null || receipt.getItems().isEmpty()) return;

        for (ReceiptItemDTO item : receipt.getItems()) {
            if (item == null) continue;

            if (item.getItem_category() == null || item.getItem_category().isBlank()) {
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
            List.of("진료", "진료비", "의료", "처방", "약")
        )
    );

    private static class ForcedItemRule {
        private final List<String> placeKeywords;
        private final String itemName;
        private final String category;
        private final List<String> dedupKeywords;

        private ForcedItemRule(
                List<String> placeKeywords,
                String itemName,
                String category,
                List<String> dedupKeywords) {

            this.placeKeywords = placeKeywords;
            this.itemName = itemName;
            this.category = category;
            this.dedupKeywords = dedupKeywords;
        }
    }

    private void applyForcedItemsIfNeeded(ReceiptDTO receipt) {

        String place = safe(receipt.getR_place());
        if (place.isEmpty()) return;

        if (receipt.getItems() == null) {
            receipt.setItems(new ArrayList<>());
        }

        String upperPlace = place.toUpperCase();

        for (ForcedItemRule rule : FORCED_ITEM_RULES) {

            boolean matched = rule.placeKeywords.stream()
                .anyMatch(k -> upperPlace.contains(k.toUpperCase()));

            if (!matched) continue;

            boolean alreadyExists = receipt.getItems().stream()
                .filter(item -> item != null && item.getItem_name() != null)
                .anyMatch(item ->
                    rule.dedupKeywords.stream()
                        .anyMatch(item.getItem_name()::contains)
                );

            if (alreadyExists) continue;

            ReceiptItemDTO item = new ReceiptItemDTO();
            item.setItem_name(rule.itemName);
            item.setItem_category(rule.category);
            item.setAi_source("RULE");
            item.setAi_confidence(1.0);
            item.setItem_price(receipt.getR_price());

            receipt.getItems().add(item);
        }
    }

    private String safe(String s) {
        return (s == null) ? "" : s.trim();
    }
}
