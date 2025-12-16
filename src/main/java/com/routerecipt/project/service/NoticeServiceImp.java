package com.routerecipt.project.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.routerecipt.project.dto.Noticedto;
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
}
