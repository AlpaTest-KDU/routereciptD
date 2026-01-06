package com.routerecipt.project.OpenAI;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Component;

import com.routerecipt.project.dto.ReceiptDTO;
import com.routerecipt.project.dto.ReceiptItemDTO;

@Component
public class ReceiptAutoItemHelper {

    /* =========================
     * 자동 아이템 규칙 정의
     * ========================= */
    private static final List<AutoItemRule> RULES = List.of(

        // 🚕 택시
        new AutoItemRule(
            List.of("택시", "TAXI"),
            "택시",
            "TRAFFIC",
            List.of("택시")
        ),

        // 🅿️ 주차
        new AutoItemRule(
            List.of("주차", "파킹", "PARKING"),
            "주차",
            "TRAFFIC",
            List.of("주차", "파킹")
        ),

        // 🏥 병원
        new AutoItemRule(
            List.of("외과", "내과", "의원", "병원", "의학과", "비뇨기과"),
            "진료비",
            "MEDICAL",
            List.of("진료", "진료비", "의료", "처방", "약")
        )
        
        // 아래 같은 양식으로 계속 추가 가능
    );

    /* =========================
     * 외부 진입 메서드
     * ========================= */
    public void applyIfNeeded(ReceiptDTO receipt) {

        if (receipt == null) return;

        String place = safe(receipt.getR_place());
        if (place.isEmpty()) return;

        if (receipt.getItems() == null) {
            receipt.setItems(new ArrayList<>());
        }

        String upperPlace = place.toUpperCase();

        for (AutoItemRule rule : RULES) {

            // 1️⃣ 장소 키워드 매칭
            if (!rule.matchPlace(upperPlace)) {
                continue;
            }

            // 2️⃣ 중복 아이템 존재 여부 확인
            if (rule.isDuplicated(receipt.getItems())) {
                continue;
            }

            // 3️⃣ 아이템 생성
            ReceiptItemDTO item = new ReceiptItemDTO();
            item.setItem_name(rule.itemName);
            item.setItem_price(receipt.getR_price());

            // ⚠️ 카테고리는 “이미 있으면 덮지 않음”
            item.setItem_category(rule.category);

            item.setAi_source("RULE");
            item.setAi_confidence(1.0);

            receipt.getItems().add(item);
        }
    }

    /* =========================
     * Rule 정의 내부 클래스
     * ========================= */
    private static class AutoItemRule {

        private final List<String> placeKeywords;
        private final String itemName;
        private final String category;
        private final List<String> dedupKeywords;

        private AutoItemRule(
                List<String> placeKeywords,
                String itemName,
                String category,
                List<String> dedupKeywords) {

            this.placeKeywords = placeKeywords;
            this.itemName = itemName;
            this.category = category;
            this.dedupKeywords = dedupKeywords;
        }

        /** 장소(r_place) 기준 매칭 */
        private boolean matchPlace(String upperPlace) {
            return placeKeywords.stream()
                    .anyMatch(k -> upperPlace.contains(k.toUpperCase()));
        }

        /** 아이템 중복 여부 확인 */
        private boolean isDuplicated(List<ReceiptItemDTO> items) {
            return items.stream()
                    .filter(Objects::nonNull)
                    .map(ReceiptItemDTO::getItem_name)
                    .filter(Objects::nonNull)
                    .anyMatch(name ->
                            dedupKeywords.stream().anyMatch(name::contains)
                    );
        }
    }

    private String safe(String s) {
        return s == null ? "" : s.trim();
    }
}
