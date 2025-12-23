package com.routerecipt.project.dto;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class OcrStreamEventDTO {
	
	private String receiptId;
	private String imagePath;
	private LocalDateTime requestTime;
	
	public OcrStreamEventDTO(String receiptId, String imagePath) {
		this.receiptId = receiptId;
		this.imagePath = imagePath;
		this.requestTime = LocalDateTime.now(); 
	}
}
