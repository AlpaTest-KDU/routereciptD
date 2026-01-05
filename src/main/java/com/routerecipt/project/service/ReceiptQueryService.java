package com.routerecipt.project.service;

import java.util.List;
import java.util.Map;

import com.routerecipt.project.dto.MyPageSummaryDTO;
import com.routerecipt.project.dto.ReceiptDTO;

public interface ReceiptQueryService {

	List<ReceiptDTO> getRecentReceipts(List<Long> r_no);
	MyPageSummaryDTO getMyPageSummary(String userId);

}
