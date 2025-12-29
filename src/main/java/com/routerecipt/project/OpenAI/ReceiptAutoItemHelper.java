package com.routerecipt.project.OpenAI;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import com.routerecipt.project.dto.ReceiptDTO;
import com.routerecipt.project.dto.ReceiptItemDTO;

public final class ReceiptAutoItemHelper {

    private ReceiptAutoItemHelper() {}

    // 주차/파킹
    private static final Pattern PARKING_PLACE_PATTERN = Pattern.compile(
            "(주차장|주차|파\\s*킹|파킹|parking)",
            Pattern.CASE_INSENSITIVE
    );

    // 택시
    private static final Pattern TAXI_PLACE_PATTERN = Pattern.compile(
            "(택시|kakao\\s*t|카카오\\s*택시|카카오t|t\\s*map\\s*t|티맵\\s*택시|티맵택시)",
            Pattern.CASE_INSENSITIVE
    );

    // 교통카드 충전 / 티머니 / 캐시비 / 이즐
    private static final Pattern TRANSPORT_CARD_TOPUP_PATTERN = Pattern.compile(
            "(교통카드\\s*충전|교통카드충전|티머니\\s*충전|티머니충전|t\\s*money\\s*충전|tmoney\\s*충전|캐시비\\s*충전|캐시비충전|이즐\\s*충전|ezl\\s*충전)",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * place 기반 자동 품목 생성 (주차비 / 택시비 / 교통카드충전)
     * - 기존 품목(items 또는 r_goods)이 있으면 생성하지 않음
     * - 생성 시 ReceiptItemDTO 1건 추가 + r_goods도 채워 UI 반영
     */
    public static void applyTransportAutoItem(ReceiptDTO receipt) {
        if (receipt == null) return;

        String place = receipt.getR_place();
        if (isBlank(place)) return;

        // 총액이 0이면 생성하지 않음
        if (receipt.getR_price() <= 0) return;

        // ✅ 기존 품목이 있으면 자동 생성 X
        if (hasAnyItem(receipt)) return;

        // 어떤 라벨을 만들지 결정
        String label = null;

        if (PARKING_PLACE_PATTERN.matcher(place).find()) {
            label = "주차비";
        } else if (TAXI_PLACE_PATTERN.matcher(place).find()) {
            label = "택시비";
        } else if (TRANSPORT_CARD_TOPUP_PATTERN.matcher(place).find()) {
            label = "교통카드충전";
        }

        if (label == null) return;

        // items 리스트 확보
        List<ReceiptItemDTO> items = receipt.getItems();
        if (items == null) {
            items = new ArrayList<>();
            receipt.setItems(items);
        }

        // 자동 품목 생성
        ReceiptItemDTO auto = new ReceiptItemDTO();
        auto.setItem_name(label);
        auto.setItem_category("교통");
        auto.setItem_price(receipt.getR_price());
        items.add(auto);

        
    }

    // ====== 여기부터가 빨간줄 원인 해결 포인트: hasAnyItem 메서드 ======
    private static boolean hasAnyItem(ReceiptDTO receipt) {
        if (receipt == null) return false;

        // 1) items 기준
        List<ReceiptItemDTO> items = receipt.getItems();
        if (items != null) {
            for (ReceiptItemDTO it : items) {
                if (it != null && !isBlank(it.getItem_name())) {
                    return true;
                }
            }
        }

        

        return false;
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
