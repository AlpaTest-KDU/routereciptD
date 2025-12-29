package com.routerecipt.project.mapper;

import java.util.List;
import java.util.Map;

public interface ReceiptResultMapper {
	 // 1. 일별 지출
    List<Map<String, Object>> selectDailyTotalByUser(String uId);

    // 2. 주간 지출
    List<Map<String, Object>> selectWeeklyTotalByUser(String uId);

    // 3. 월간 지출
    List<Map<String, Object>> selectMonthlyTotalByUser(String uId);

    // 4. 성별별 지출
    List<Map<String, Object>> selectTotalByGender();

    // 5. 나의 평균
    int selectMyAverage(String uId);

    // 6. 전체 평균
    int selectAllAverage();
}
