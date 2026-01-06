package com.routerecipt.project.ocr;



import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.routerecipt.project.dto.ReceiptDTO;
import com.routerecipt.project.dto.ReceiptItemDTO;
import com.routerecipt.project.mapper.ReceiptMapper;
import com.routerecipt.project.service.RankingService;

import lombok.RequiredArgsConstructor;

/**
 * 영수증 + 영수증 품목 저장 전용 Service
 *
 * 역할:
 *  - receipt 테이블 저장
 *  - receipt_item 테이블 저장
 *  - 하나의 트랜잭션으로 처리
 *
 * ❌ OCR, AI 분류, 비즈니스 계산 로직은 여기서 하지 않음
 */
@Service
@RequiredArgsConstructor
public class ReceiptSaveService {
	
	/** 영수증 / 품목 DB 접근 Mapper */
    private final ReceiptMapper receiptMapper;

    @Autowired
    /** 소비 랭킹 집계 서비스  */
    private final RankingService rankingService;

    @Transactional
    public Long saveReceiptWithItems(String userId, ReceiptDTO receipt) {
    	// ---------------- 1. 파라미터 검증 ----------------
        if (receipt == null) throw new IllegalArgumentException("receipt is null");
        if (userId == null || userId.isBlank()) throw new IllegalArgumentException("userId is blank");
        
        
        // ---------------- 2. receipt 테이블 저장 ----------------
        // 로그인 사용자 ID 세팅
        receipt.setR_u(userId);
        
        // insert 후 r_no 자동 세팅 기대
        receiptMapper.insertReceipt(receipt);
        
        // ---------------- 3. 생성된 PK 검증 ----------------
        Long rNo = receipt.getR_no();
        if (rNo == null) {
            throw new IllegalStateException("insertReceipt 후 r_no가 null 입니다. (generatedKeys 설정 확인)");
        }

        // ---------------- 4. receipt_item 테이블 저장 ----------------
        List<ReceiptItemDTO> items = receipt.getItems();
        if (items != null && !items.isEmpty()) {
        	// FK(r_no) 주입
            for (ReceiptItemDTO it : items) {
                it.setR_no(rNo);
            }
            // 품목 batch insert
            receiptMapper.insertReceiptItems(rNo, items);
//            rankingService.recordSpending(receipt.getCategory(), receipt.getR_price(), userId, receipt.getR_date());
        }
        // ---------------- 5. 생성된 영수증 PK 반환 ----------------
        return rNo;
    }
}
