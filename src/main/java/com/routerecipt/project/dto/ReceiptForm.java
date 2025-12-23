package com.routerecipt.project.dto;

import java.util.List;
import java.util.Map;

public class ReceiptForm {
	private List<ReceiptDTO> receipts;
	
	public List<ReceiptDTO> getReceipts(){
		return receipts;
	}
	
	public void setReceipts(List<ReceiptDTO> receipts) {
		this.receipts = receipts;
	}
}
