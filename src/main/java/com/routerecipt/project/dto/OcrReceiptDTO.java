package com.routerecipt.project.dto;

import java.util.Date;

import lombok.Data;

@Data
public class OcrReceiptDTO {
	private String shop;
	private Date date;
	private int total;
}
