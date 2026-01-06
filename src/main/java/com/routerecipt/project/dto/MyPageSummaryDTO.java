package com.routerecipt.project.dto;

import lombok.Builder;
import lombok.Data;


/**
 * 마이페이지 요약 정보 DTO
 *
 * 역할:
 *  - 사용자의 소비 현황을 한눈에 보여주는 대시보드 데이터
 *  - 마이페이지 상단 요약 영역에 사용
 */
@Data
@Builder
public class MyPageSummaryDTO {
	
	// 조회 기간 내 영수증 개수
    private int receiptCount;
    
    //조회 기간 내 총 지출 금액
    private long totalAmount;
    
    // 가장 많이 소비한 카테고리
    private String topCategory;
    
    //이전 기간 대비 영수증 개수 비교 문구
    private String compareCountText;
    
    // 가장 큰 금액의 영수증 요약
    private String topAmountReceipt;
}
