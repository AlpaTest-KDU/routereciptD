package com.routerecipt.project.util;

import java.time.*;

public class StreamTimeUtil {
	
	public static long startOfMonthMillis(YearMonth ym) {
		return ym.atDay(1)
				.atStartOfDay(ZoneId.systemDefault())
				.toInstant()
				.toEpochMilli();
	}
	
	public static long endOfMonthMillis(YearMonth ym) {
		return ym.atEndOfMonth()
				.atTime(LocalTime.MAX)
				.atZone(ZoneId.systemDefault())
				.toInstant()
				.toEpochMilli();
	}
	
	// Stream ID 시작값
	public static String startId(YearMonth ym) {
		return startOfMonthMillis(ym) + "-0";
	}
	
	// Stream ID 끝값
	public static String endId(YearMonth ym) {
		return endOfMonthMillis(ym) + "-9999";
	}
	
}
