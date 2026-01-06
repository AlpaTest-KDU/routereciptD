package com.routerecipt.project.service;

import java.util.List;
import java.util.Map;

import com.routerecipt.project.dto.MyPageSummaryDTO;
import com.routerecipt.project.dto.ReceiptDTO;

public interface ReceiptQueryService {
	 // 월별 저장된 영수증
    List<ReceiptDTO> getSavedReceiptsDate(String userId, String yearMonth);

    // 카테고리별 메뉴 Map
    Map<String, List<String>> buildMenuMap(List<ReceiptDTO> receipts);

    // 달력 데이터
    List<Integer> buildCalendar(String yearMonth);

    // 최근(임시) 영수증 + 아이템
	List<ReceiptDTO> getRecentReceipts(List<Long> r_no);
	MyPageSummaryDTO getMyPageSummary(String userId);

}
