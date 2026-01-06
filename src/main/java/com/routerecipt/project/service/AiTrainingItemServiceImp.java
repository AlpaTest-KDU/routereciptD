package com.routerecipt.project.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.routerecipt.project.dto.AiTrainingItemDTO;
import com.routerecipt.project.mapper.AiTrainingItemMapper;

import lombok.RequiredArgsConstructor;


/**
 * AI 학습용 데이터 저장 서비스 구현체
 *
 * - 영수증 확정 시 AI 예측값과 최종 값을 비교
 * - 교정 여부(corrected_yn) 판단
 * - 학습 반영 여부(used_for_training) 초기값 설정
 * - DB에 학습 데이터 저장
 */
@Service
@RequiredArgsConstructor
public class AiTrainingItemServiceImp implements AiTrainingItemService {
	
	// AI 학습 데이터 DB 저장용 Mapper
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
