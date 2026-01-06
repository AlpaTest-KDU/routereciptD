package com.routerecipt.project.dto;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import com.routerecipt.project.common.Gender;

import lombok.Data;

/**
 * 영수증(Receipt) DTO
 *
 * 역할:
 *  - 영수증 1건의 정보를 표현하는 핵심 도메인 DTO
 *  - OCR 결과, 수기 입력, DB 조회 결과를 공통 포맷으로 전달
 *  - ReceiptItemDTO 리스트를 포함하는 부모 객체
 */
@Data
public class ReceiptDTO {

	private Long r_no;						// 영수증 고유 번호
	private String r_u;						// 사용자 ID (영수증 소유자)	
	private String r_place;					// 상호명 / 가게 이름
	private Integer r_price;				// 총 결제 금액
	@DateTimeFormat(pattern = "yyyy-MM-dd")	// 거래 날짜(yyyy-MM-dd 형식으로 바인딩)
	private LocalDate r_date;				
	
	private Gender gender;					// 사용자 성별
	
	private List<ReceiptItemDTO> items;		// 영수증에 포함된 상품(아이템) 목록
}
