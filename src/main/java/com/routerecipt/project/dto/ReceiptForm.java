package com.routerecipt.project.dto;

import java.util.List;


/**
 * 다중 영수증 폼 바인딩용 DTO
 *
 * 역할:
 *  - 여러 개의 ReceiptDTO를 하나의 요청 객체로 묶기 위한 래퍼 클래스
 *  - Spring MVC에서 리스트 형태의 폼 데이터를 정상적으로 바인딩하기 위해 사용
 *
 * 사용 예:
 *  receipts[0].r_place
 *  receipts[1].r_place
 */
public class ReceiptForm {
	private List<ReceiptDTO> receipts;		// 폼에서 전달되는 영수증 목록
	
	
	// 수증 목록 반환
	public List<ReceiptDTO> getReceipts(){
		return receipts;
	}
	
	// 영수증 목록 설정
	public void setReceipts(List<ReceiptDTO> receipts) {
		this.receipts = receipts;
	}
}
