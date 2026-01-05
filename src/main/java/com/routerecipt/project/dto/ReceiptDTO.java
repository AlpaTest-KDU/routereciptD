package com.routerecipt.project.dto;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import com.routerecipt.project.common.Gender;

import lombok.Data;

@Data
public class ReceiptDTO {

	private Long r_no;
	private String r_u;
	private String r_place;
	private Integer r_price;
	@DateTimeFormat(pattern = "yyyy-MM-dd")
	private LocalDate r_date;
	
	private Gender gender;
	
	private List<ReceiptItemDTO> items;
}
