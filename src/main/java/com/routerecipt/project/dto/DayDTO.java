package com.routerecipt.project.dto;

import java.util.List;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class DayDTO {

    private int day;                    // 1 ~ 31
    private List<ReceiptDTO> receipts;  // 해당 날짜의 영수증
}
