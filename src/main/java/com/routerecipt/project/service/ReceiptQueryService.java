package com.routerecipt.project.service;

import java.util.List;

import com.routerecipt.project.dto.ReceiptDTO;

public interface ReceiptQueryService {
	List<ReceiptDTO> getRecentReceipts(List<Long> r_no);
}
