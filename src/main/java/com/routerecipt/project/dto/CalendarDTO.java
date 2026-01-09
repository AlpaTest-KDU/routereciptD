package com.routerecipt.project.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.YearMonth;
import java.util.List;

@Getter
@Setter
public class CalendarDTO {
    private YearMonth month;
    private List<ReceiptDTO> receipt;
}
