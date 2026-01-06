package com.routerecipt.project.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.routerecipt.project.dto.Noticedto;
import com.routerecipt.project.dto.Criteria;
import com.routerecipt.project.mapper.NoticeMapper;


/**
 * 공지사항(Notice) 관련 비즈니스 로직을 수행하는 서비스 구현체
 *
 * - NoticeService 인터페이스 구현
 * - Controller와 Mapper(DB) 사이의 중간 계층
 */
@Service
public class NoticeServiceImp implements NoticeService{
	
	// 공지사항 DB 접근을 담당하는 Mapper
	@Autowired
	private NoticeMapper noticeMapper;
	
	// 공지사항 등록
	@Override
	public void NoticeRegister(Noticedto n) {
		noticeMapper.NoticeRegister(n);
	}
	
	// 공지사항 전체 목록 조회 (페이징 없음)
	@Override
	public List<Noticedto> NoticeShow() {
		return noticeMapper.NoticeShow();
	}
	
	// 공지사항 상세 조회
	@Override
	public Noticedto findById(int n_id) {
		return noticeMapper.findById(n_id);
	}

	// 공지사항 총 개수 조회 (페이징 계산용)
	@Override
	public int getTotalNoticeCount(Criteria criteria) {
		return noticeMapper.getTotalNoticeCount(criteria);
	}
	
	// 공지사항 페이징 목록 조회
	@Override
	public List<Noticedto> NoticeShowWithPage(Criteria criteria) {
		return noticeMapper.NoticeShowWithPage(criteria);
	}
}
