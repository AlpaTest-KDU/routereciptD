// src/main/java/com/routerecipt/project/receipt/ReceiptResultService.java
package com.routerecipt.project.receipt;

import org.springframework.stereotype.Service;

import com.routerecipt.project.dto.ReceiptAnalysisStatsdto;
import com.routerecipt.project.mapper.ReceiptResultMapper;

import lombok.RequiredArgsConstructor;


/**
 * 영수증/지출 분석 결과(통계) 서비스
 *
 * 역할:
 *  - 분석 페이지에서 필요한 통계 데이터를 DB에서 조회해
 *    ReceiptAnalysisStatsdto에 담아 반환한다.
 *
 * 특징:
 *  - 화면(페이지) 단위로 필요한 통계가 다르므로 메서드를 분리해 제공한다.
 */
@Service
@RequiredArgsConstructor
public class ReceiptResultService {
	
	// 통계 조회 전용 Mapper
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
        dto.setMyAvg(0);
        if (uId != null) {
            dto.setMyAvg(receiptresultmapper.selectMyAverage(uId));
        }
        dto.setAllAvg(receiptresultmapper.selectAllAverage());
        return dto;
    }
}
