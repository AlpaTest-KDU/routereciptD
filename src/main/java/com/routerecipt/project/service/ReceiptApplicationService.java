package com.routerecipt.project.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.routerecipt.project.dto.ReceiptDTO;

public interface ReceiptApplicationService {
	
	  /* 영수증 + 아이템 + AI 분류 저장 */
    void saveReceiptWithItems(ReceiptDTO receipt);

    /* 월별 영수증 조회 */
    List<ReceiptDTO> getSavedReceiptsDate(String userId, String yearMonth);

    /* 카테고리별 메뉴 Map 생성 */
    Map<String, List<String>> buildMenuMap(List<ReceiptDTO> receipts);

    /* 달력 데이터 생성 */
    List<Integer> buildCalendar(String yearMonth);
    


    void confirmReceipt(
        Long r_no,
        String r_place,
        LocalDate r_date,
        Integer r_price,
        List<String> item_names,
        List<Integer> item_prices,
        List<String> item_categories
    );
}


