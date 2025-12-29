package com.routerecipt.project.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

// 달력 셀(하루) 표현용 DTO
@Getter
@Setter
public class DayDTO {
    private int day;                       // 1~31
    private List<ReceiptDTO> receipts;      // 그 날짜의 영수증들
}
