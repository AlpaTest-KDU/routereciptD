package com.routerecipt.project.service;

import java.time.LocalDate;
import java.util.List;

import com.routerecipt.project.dto.ReceiptDTO;

/**
 * 영수증(Receipt) Command(쓰기) 서비스 인터페이스
 *
 * - 데이터 변경이 발생하는 기능(저장/확정)만 정의한다.
 * - 구현체에서는 영수증/아이템 DB 저장, 업데이트, 확정 상태 변경 등을 수행한다.
 * - 보통 트랜잭션으로 처리하여 영수증과 아이템이 함께 일관되게 반영되도록 한다.
 */
public interface ReceiptCommandService {
	
	// 영수증(헤더) + 아이템(상세) 목록을 함께 저장한다.
    void saveReceiptWithItems(ReceiptDTO receipt);
    
    // 영수증 확정 처리
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
    // OCR 상태 변경 (비동기 전용)
    // =========================
    void updateOcrStatus(Long r_no, String status);

    void updateOcrStatusByImagePath(String imagePath, String status);
}
