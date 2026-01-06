package com.routerecipt.project.service;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.routerecipt.project.OpenAI.OpenAiOcrAssisService;
import com.routerecipt.project.OpenAI.ReceiptAutoItemHelper;
import com.routerecipt.project.dto.ItemCategory;
import com.routerecipt.project.dto.ReceiptDTO;
import com.routerecipt.project.dto.ReceiptItemDTO;
import com.routerecipt.project.dto.UploadResult;
import com.routerecipt.project.ocr.OcrService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReceiptServiceImp implements ReceiptService {

    private final OcrService ocrService;
    private final OpenAiOcrAssisService openAiOcrAssisService; // OCR 보조
    private final ReceiptAutoItemHelper receiptAutoItemHelper; // 강제 아이템
    private final ReceiptApplicationService receiptApplicationService;

    @Override
    public UploadResult uploadReceipts(List<MultipartFile> files, String userId) {

        UploadResult result = new UploadResult();
        List<Long> successNos = new ArrayList<>();

        int success = 0;
        int fail = 0;

        for (MultipartFile file : files) {
            try {
                // 1️⃣ 파일 유효성
                if (file == null || file.isEmpty()) {
                    fail++;
                    continue;
                }

                // 2️⃣ OCR 호출
                JSONObject json = ocrService.callClovaOCR(file);
                if (json == null) {
                    fail++;
                    continue;
                }

                // 3️⃣ OCR 파싱 → ReceiptDTO 생성
                ReceiptDTO receipt = ocrService.parseReceiptWithAssist(json, file);
                if (receipt == null) {
                    fail++;
                    continue;
                }

                // 4️⃣ 사용자 아이디 세팅
                receipt.setR_u(userId);

                // 5️⃣ r_place 기반 강제 아이템 RULE 적용
                applyForcedItemsIfNeeded(receipt);

                // 6️⃣ DB 저장 전 item_category 최종 보정
                applyCategoryFallback(receipt);

                // 7️⃣ 영수증 + 아이템 저장
                receiptApplicationService.saveReceiptWithItems(receipt);

                successNos.add(receipt.getR_no());
                success++;

            } catch (Exception e) {
                e.printStackTrace();
                fail++;
            }
        }

        result.setSuccessReceiptNos(successNos);
        result.setSuccessCount(success);
        result.setFailCount(fail);
        return result;
    }

    // ======================================================
    // item_category NOT NULL 보장
    // ======================================================
    private void applyCategoryFallback(ReceiptDTO receipt) {

        if (receipt.getItems() == null || receipt.getItems().isEmpty()) {
            return;
        }

        for (ReceiptItemDTO item : receipt.getItems()) {
            if (item == null) continue;

            if (item.getItem_category() == null || item.getItem_category().isBlank()) {
                item.setItem_category(ItemCategory.ETC.name());
                item.setAi_source("FALLBACK");
                item.setAi_confidence(0.0);
            }
        }
    }

    // =========================
    // 자동 아이템 추가 규칙
    // =========================
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

        public ForcedItemRule(
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

            boolean alreadyInGroup = receipt.getItems().stream()
                    .filter(item -> item != null && item.getItem_name() != null)
                    .anyMatch(item ->
                            rule.dedupKeywords.stream().anyMatch(item.getItem_name()::contains)
                    );

            if (alreadyInGroup) continue;

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
