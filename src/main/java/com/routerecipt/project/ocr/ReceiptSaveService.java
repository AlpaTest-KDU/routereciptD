package com.routerecipt.project.ocr;



import java.util.List;

import com.routerecipt.project.service.RankingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.routerecipt.project.dto.ReceiptDTO;
import com.routerecipt.project.dto.ReceiptItemDTO;
import com.routerecipt.project.mapper.ReceiptMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReceiptSaveService {

    private final ReceiptMapper receiptMapper;

    @Autowired
    private final RankingService rankingService;

    @Transactional
    public Long saveReceiptWithItems(String userId, ReceiptDTO receipt) {
        if (receipt == null) throw new IllegalArgumentException("receipt is null");
        if (userId == null || userId.isBlank()) throw new IllegalArgumentException("userId is blank");

        // receipt 테이블 저장
        receipt.setR_u(userId);
        receiptMapper.insertReceipt(receipt);

        Long rNo = receipt.getR_no();
        if (rNo == null) {
            throw new IllegalStateException("insertReceipt 후 r_no가 null 입니다. (generatedKeys 설정 확인)");
        }

        // receipt_item 저장
        List<ReceiptItemDTO> items = receipt.getItems();
        if (items != null && !items.isEmpty()) {
            for (ReceiptItemDTO it : items) {
                it.setR_no(rNo);
            }
            receiptMapper.insertReceiptItems(rNo, items);
            rankingService.recordSpending(receipt.getCategory(), receipt.getR_price(), userId, receipt.getR_date());
        }

        return rNo;
    }
}
