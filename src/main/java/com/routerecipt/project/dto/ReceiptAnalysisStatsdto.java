package com.routerecipt.project.dto;

import java.util.List;
import java.util.Map;

import lombok.Data;

// DTO(Data Transfer Object) 클래스 - 영수증 분석 통계 데이터 전송용
@Data
public class ReceiptAnalysisStatsdto {

    // 1) daily - 일별 소비 데이터
    private List<Map<String, Object>> dailyData;

    // 2) weekly - 주간 소비 데이터
    private List<Map<String, Object>> weeklyData;

    // 3) monthly - 월별 소비 데이터
    private List<Map<String, Object>> monthlyData;

    // 4) gender - 성별 (전체 통계라면 사용, 개인만이면 선택)
    private List<Map<String, Object>> genderData;

    // 5) 나의 평균 / 전체 평균
    private int myAvg;
    private int allAvg;
}
