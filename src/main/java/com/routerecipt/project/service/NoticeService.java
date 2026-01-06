package com.routerecipt.project.service;

import java.util.List;

import com.routerecipt.project.dto.Criteria;
import com.routerecipt.project.dto.Noticedto;


/**
 * 공지사항(Notice) 관련 비즈니스 로직을 정의하는 서비스 인터페이스
 *
 * - 공지사항 등록
 * - 공지사항 목록 조회
 * - 공지사항 상세 조회
 * - 공지사항 페이징 처리
 */
public interface NoticeService {
	void NoticeRegister (Noticedto n);			// 공지사항 등록
	List<Noticedto>NoticeShow();				// 공지사항 전체 목록 조회 (페이징 미적용)
	Noticedto findById(int n_id);				// 공지사항 상세 조회
	int getTotalNoticeCount(Criteria criteria);	// 공지사항 총 개수 조회 (페이징 계산용)
	List<Noticedto> NoticeShowWithPage(Criteria criteria);	// 공지사항 페이징 목록 조회
}
