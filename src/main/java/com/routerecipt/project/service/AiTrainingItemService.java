package com.routerecipt.project.service;

import com.routerecipt.project.dto.AiTrainingItemDTO;


/**
 * AI 학습용 데이터 관리를 위한 서비스 인터페이스
 *
 * - AI 모델 학습에 사용될 데이터를 저장하는 기능의 규약
 * - 구현체에서는 DB, 파일, 외부 저장소 등 다양한 방식으로 저장 가능
 */
public interface AiTrainingItemService {
	
	// AI 학습용 데이터 저장
	void saveTrainingItem(AiTrainingItemDTO ai);
	
}
