package com.routerecipt.project.dto;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class AiTrainingItemDTO {
	
	private Long train_id;
	
	// 학습 입력
	private String item_text;
	
	// AI 예측 정보
	private String predicted_label;
	private Double predicted_confidence;
	private String ai_source; // AI / RULE
	private String model_version;
	
	// 사용자 확정
	private String final_label;
	private String corrected_yn;
	
	// 학습 관리
	private String used_for_training;
	private LocalDateTime trained_at;
	
	// 메타
	private LocalDateTime created_at;
	
	
}
