package com.routerecipt.project.receipt;

import org.springframework.stereotype.Service;

import com.routerecipt.project.dto.ReceiptAnalysisStatsdto;
import com.routerecipt.project.mapper.ReceiptResultMapper;

import lombok.RequiredArgsConstructor;


@Service
@RequiredArgsConstructor
public class ReceiptResultService {
	 private final ReceiptResultMapper receiptresultmapper;
	 
	    public ReceiptAnalysisStatsdto getStats(String uId) {

	    	ReceiptAnalysisStatsdto radto = new ReceiptAnalysisStatsdto();

	        // 1) 일별
	    	radto.setDailyData(receiptresultmapper.selectDailyTotalByUser(uId));

	        // 2) 주별
	    	radto.setWeeklyData(receiptresultmapper.selectWeeklyTotalByUser(uId));

	        // 3) 월별
	    	radto.setMonthlyData(receiptresultmapper.selectMonthlyTotalByUser(uId));

	        // 4) 성별
	    	radto.setGenderData(receiptresultmapper.selectTotalByGender());

	        // 5) 평균
	    	radto.setMyAvg(receiptresultmapper.selectMyAverage(uId));
	    	radto.setAllAvg(receiptresultmapper.selectAllAverage());

	        return radto;
	    }
}
