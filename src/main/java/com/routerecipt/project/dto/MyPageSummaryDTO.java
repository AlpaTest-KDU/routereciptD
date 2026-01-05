package com.routerecipt.project.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MyPageSummaryDTO {
    private int receiptCount;
    private long totalAmount;
    private String topCategory;
    private String compareCountText;
    private String topAmountReceipt;
}
