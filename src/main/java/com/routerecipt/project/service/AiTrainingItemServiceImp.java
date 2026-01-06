package com.routerecipt.project.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.routerecipt.project.dto.AiTrainingItemDTO;
import com.routerecipt.project.mapper.AiTrainingItemMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AiTrainingItemServiceImp implements AiTrainingItemService {
	
	private final AiTrainingItemMapper aiTrainingItemMapper;
	
	/* 영수증 확정 시 학습 데이터 저장 */
	
	@Override
	@Transactional
	public void saveTrainingItem(AiTrainingItemDTO ai) {
		
		// corrected_yn 계산
		if (ai.getPredicted_label() != null && ai.getPredicted_label().equals(ai.getFinal_label())) {
			ai.setCorrected_yn("N");
		} else {
			ai.setCorrected_yn("Y");
		}
		
		// 최초 저장 시 학습 미반영 상태
		ai.setUsed_for_training("N");
		
		// DB 저장
		aiTrainingItemMapper.insertAiTrainingItem(ai);
		
	}
}
