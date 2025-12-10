package com.routerecipt.project.service;

import org.springframework.beans.factory.annotation.Autowired;

import com.routerecipt.project.mapper.ReciptMapper;

public class ReceiptServiceImp implements ReceiptService {
	
	@Autowired
	private ReciptMapper receiptMapper;
}
