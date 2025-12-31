// src/main/java/com/routerecipt/project/receipt/ReceiptResultService.java
package com.routerecipt.project.receipt;

import org.springframework.stereotype.Service;

import com.routerecipt.project.dto.ReceiptAnalysisStatsdto;
import com.routerecipt.project.mapper.ReceiptResultMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReceiptResultService {

    private final ReceiptResultMapper receiptresultmapper;

    // /user/analysisMonthlyPage : 개인 사용자
    public ReceiptAnalysisStatsdto getAnalysisMonthlyPageStats(String uId) {
        ReceiptAnalysisStatsdto dto = new ReceiptAnalysisStatsdto();
        dto.setDailyData(receiptresultmapper.selectDailyTotalByUser(uId));
        dto.setWeeklyData(receiptresultmapper.selectWeeklyTotalByUser(uId));
        dto.setMonthlyData(receiptresultmapper.selectMonthlyTotalByUser(uId));
        return dto;
    }

    // /user/analysisPage : 전체 사용자
    public ReceiptAnalysisStatsdto getAnalysisPageStats(String uId) {
        ReceiptAnalysisStatsdto dto = new ReceiptAnalysisStatsdto();
        dto.setGenderData(receiptresultmapper.selectTotalByGender());
        dto.setMyAvg(receiptresultmapper.selectMyAverage(uId));
        dto.setAllAvg(receiptresultmapper.selectAllAverage());
        return dto;
    }
}
