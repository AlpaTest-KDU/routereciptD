package com.routerecipt.project.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.routerecipt.project.dto.ReceiptDTO;

public interface ReceiptApplicationService {

    // =========================
    // Write (저장/확정)
    // =========================

    // 영수증 + 아이템 저장
    void saveReceiptWithItems(ReceiptDTO receipt);

    // 영수증 확정(수정 반영)
    void confirmReceipt(
            Long r_no,
            String r_place,
            LocalDate r_date,
            Integer r_price,
            List<String> item_names,
            List<Integer> item_prices,
            List<String> item_categories
    );

    // =========================
    // Read (조회/화면 구성)
    // =========================

    List<ReceiptDTO> getSavedReceiptsDate(String userId, String yearMonth);

    Map<String, List<String>> buildMenuMap(List<ReceiptDTO> receipts);

    List<Integer> buildCalendar(String yearMonth);

    List<ReceiptDTO> getRecentReceipts(List<Long> r_no);

    // =========================
    // OCR 상태 관리 (비동기 전용)
    // =========================

    // OCR 성공/실패 시 상태 업데이트
    void updateOcrStatus(Long r_no, String status);

    // imagePath 기준 OCR 실패 처리용
    void updateOcrStatusByImagePath(String imagePath, String status);
}
