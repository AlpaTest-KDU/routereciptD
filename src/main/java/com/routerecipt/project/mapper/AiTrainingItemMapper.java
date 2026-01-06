package com.routerecipt.project.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.routerecipt.project.dto.AiTrainingItemDTO;

@Mapper
public interface AiTrainingItemMapper {
	
	// 학습 데이터 저장
	void insertAiTrainingItem(AiTrainingItemDTO ai);
	
	// 재학습 대상 조회
	List<AiTrainingItemDTO> selectTrainingTargets();
	
	// 재학습 완료 처리
	void markAsTrained(List<Long> train_ids);
}
