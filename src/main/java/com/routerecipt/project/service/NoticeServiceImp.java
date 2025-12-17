package com.routerecipt.project.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.routerecipt.project.dto.Noticedto;
import com.routerecipt.project.dto.Criteria;
import com.routerecipt.project.mapper.NoticeMapper;

@Service
public class NoticeServiceImp implements NoticeService{
	
	@Autowired
	private NoticeMapper noticeMapper;
	
	@Override
	public void NoticeRegister(Noticedto n) {
		noticeMapper.NoticeRegister(n);
	}
	
	@Override
	public List<Noticedto> NoticeShow() {
		return noticeMapper.NoticeShow();
	}
	
	@Override
	public Noticedto findById(int n_id) {
		return noticeMapper.findById(n_id);
	}

	@Override
	public int getTotalNoticeCount(Criteria criteria) {
		return noticeMapper.getTotalNoticeCount(criteria);
	}

	@Override
	public List<Noticedto> NoticeShowWithPage(Criteria criteria) {
		return noticeMapper.NoticeShowWithPage(criteria);
	}
}
