package com.routerecipt.project.service;

import com.routerecipt.project.dto.ReceiptDTO;

/**
 * 영수증 대표 카테고리 선택 서비스 인터페이스
 *
 * - 하나의 영수증에 포함된 여러 아이템을 종합하여
 *   영수증을 대표하는 카테고리 하나를 결정하는 역할
 * - 구현체에서는 다수결, 금액 합계, 우선순위 등의
 *   다양한 기준을 적용할 수 있다.
 */
public interface ReceiptCategoryService {
	
	// 영수증의 대표 카테고리를 선택한다.
	String pickCategory(ReceiptDTO receipt);
	
}
