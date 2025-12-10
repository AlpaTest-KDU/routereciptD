package com.routerecipt.project.service;

import org.springframework.beans.factory.annotation.Autowired;

import com.routerecipt.project.mapper.NoticeMapper;

public class NoticeServiceImp implements NoticeService{
	
	@Autowired
	private NoticeMapper noticeMapper;
}
