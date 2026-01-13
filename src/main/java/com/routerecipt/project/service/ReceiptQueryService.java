package com.routerecipt.project.service;

import java.time.YearMonth;
import java.util.List;
import java.util.Map;

import com.routerecipt.project.dto.MyPageSummaryDTO;
import com.routerecipt.project.dto.ReceiptDTO;


/**
 * 영수증(Receipt) Query(조회) 서비스 인터페이스
 *
 * - DB 상태를 변경하지 않는 "읽기(Read)" 기능만 정의한다.
 * - 영수증 목록 조회뿐만 아니라, 화면 구성을 위한 데이터 가공(달력/메뉴맵/요약)을 포함한다.
 * - ReceiptCommandService(쓰기)와 분리하여 책임을 명확히 한다.
 */
public interface ReceiptQueryService {
	 // 월별 저장된 영수증
    List<ReceiptDTO> getSavedReceiptsDate(String userId, String yearMonth);

    // 최근(임시) 영수증 + 아이템
	List<ReceiptDTO> getRecentReceipts(List<Long> r_no);
	MyPageSummaryDTO getMyPageSummary(String userId);
	
	List<ReceiptDTO> getRecentReceiptsByUser(String userId, int limit);

}
