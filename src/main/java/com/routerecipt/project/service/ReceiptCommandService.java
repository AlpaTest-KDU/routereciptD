package com.routerecipt.project.service;

import java.time.LocalDate;
import java.util.List;

import com.routerecipt.project.dto.ReceiptDTO;

/**
 * 영수증(Receipt) Command(쓰기) 서비스 인터페이스
 *
 * ✔ DB 변경 전용
 * ✔ OCR 비동기 처리 / 사용자 확정 처리 명확 분리
 */
public interface ReceiptCommandService {

    /* =====================================================
     * 🚫 LEGACY (더 이상 OCR에서 사용하지 않음)
     * ===================================================== */
    @Deprecated
    void saveReceiptWithItems(ReceiptDTO receipt);

    /* =====================================================
     * ✅ 사용자 확정 (수기 수정 후 저장)
     * ===================================================== */
    void confirmReceipt(
            Long r_no,
            String r_place,
            LocalDate r_date,
            Integer r_price,
            List<String> item_names,
            List<Integer> item_prices,
            List<String> item_categories
    );

    /* =====================================================
     * 🔥 OCR 비동기 전용 (핵심)
     * ===================================================== */

    /** OCR 시작 시 PENDING receipt 생성 */
    void insertPendingReceipt(ReceiptDTO receipt);

    /** OCR 상태 변경 */
    void updateOcrStatus(Long r_no, String status);
    void updateOcrStatusByImagePath(String imagePath, String status);

    /** OCR 결과 → receipt 헤더 UPDATE */
    void updateReceiptBasic(
            Long r_no,
            String r_place,
            LocalDate r_date,
            Integer r_price
    );

    /** 기존 아이템 삭제 (재처리 대비) */
    void deleteItemsByReceiptNo(Long r_no);

    /** OCR 결과 아이템 INSERT */
    void insertReceiptItems(ReceiptDTO receipt);
}
