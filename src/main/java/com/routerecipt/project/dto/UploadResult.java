package com.routerecipt.project.dto;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UploadResult {

    private List<Long> successReceiptNos;
    private int successCount;
    private int failCount;

    public boolean isAllFailed() {
        return successCount == 0;
    }

    public boolean isPartialSuccess() {
        return successCount > 0 && failCount > 0;
    }
}
