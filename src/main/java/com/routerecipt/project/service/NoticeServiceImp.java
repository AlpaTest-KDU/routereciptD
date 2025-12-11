package com.routerecipt.project.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.routerecipt.project.mapper.NoticeMapper;

@Service
public class NoticeServiceImp implements NoticeService{
	
	@Autowired
	private NoticeMapper noticeMapper;
}
