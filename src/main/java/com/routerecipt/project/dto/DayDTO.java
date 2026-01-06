package com.routerecipt.project.dto;


import java.util.List;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;


/**
 * 날짜별 영수증 묶음 DTO
 *
 * 역할:
 *  - 월 단위 조회에서 하루(일자) 기준으로 영수증을 그룹화
 *  - 캘린더 UI, 일별 지출 내역 화면에 사용
 */
@Getter
@Setter
@ToString
public class DayDTO {
	

    
	// 날짜(1 ~ 31 범위)
    private int day;                    
    
    
     // 해당 날짜에 속한 영수증 목록
    
    private List<ReceiptDTO> receipts;  // 해당 날짜의 영수증

}
