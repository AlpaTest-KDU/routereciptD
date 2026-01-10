package com.routerecipt.project.service;

import java.time.LocalDate;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.routerecipt.project.dto.ReceiptDTO;
import com.routerecipt.project.dto.ReceiptItemDTO;
import com.routerecipt.project.mapper.ReceiptMapper;

import lombok.RequiredArgsConstructor;

/**
 * 영수증 Command(쓰기) 서비스 구현체
 *
 * - 영수증/아이템의 DB 저장 및 확정(수정) 처리를 담당한다.
 * - AI 분류 결과(item_category, ai_source, ai_confidence 등)는
 *   상위 계층(딥러닝/애플리케이션 서비스)에서 채워져 넘어온다고 가정하고,
 *   이 클래스는 "DB 반영"에만 집중한다.
 */
@Service
@RequiredArgsConstructor
public class ReceiptCommandServiceImp implements ReceiptCommandService {

    private static final Logger log = LoggerFactory.getLogger(ReceiptCommandServiceImp.class);

    private final ReceiptMapper receiptMapper;

    /* =====================================================
     * 영수증 + 아이템 저장
     * - item_category/ai_source/ai_confidence 등은 딥러닝 파이프라인에서 이미 채워져 들어온다고 가정
     * - 여기서는 DB 저장만 책임
     * ===================================================== */
    @Override
    @Transactional
    public void saveReceiptWithItems(ReceiptDTO receipt) {

        if (receipt == null) return;

        // DB 보호용 최소 보정
        if (receipt.getR_date() == null) receipt.setR_date(LocalDate.now());
        if (receipt.getR_price() == 0) receipt.setR_price(0);

        // 1) 영수증 저장
        receiptMapper.insertReceipt(receipt);
        Long r_no = receipt.getR_no();

        // 2) 아이템 저장
        List<ReceiptItemDTO> items = receipt.getItems();
        if (r_no != null && items != null && !items.isEmpty()) {
            for (ReceiptItemDTO it : items) {
                it.setR_no(r_no);

                // item_name 없으면 스킵 (정책에 맞게 변경 가능)
                if (it.getItem_name() == null || it.getItem_name().isBlank()) {
                    log.warn("Skip item with blank name. r_no={}, item={}", r_no, it);
                    continue;
                }

                receiptMapper.insertItem(it);
            }
        }
    }

    /* =====================================================
     * 분석 후 수정을 하기 위함 (confirm)
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
        // 1) receipt 기본 정보 업데이트
        receiptMapper.updateReceiptBasic(r_no, r_place, r_date, r_price);

        // 2) 기존 items 삭제
        receiptMapper.deleteItemsByReceiptNo(r_no);

        // 3) 새 items insert
        receiptMapper.insertItemsBatch(r_no, item_names, item_prices, item_categories);
    }
    
    @Override
    public void updateOcrStatus(Long r_no, String status) {
        receiptMapper.updateOcrStatus(r_no, status);
    }

    @Override
    public void updateOcrStatusByImagePath(String imagePath, String status) {
        receiptMapper.updateOcrStatusByImagePath(imagePath, status);
    }
}
