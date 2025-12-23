package com.routerecipt.project.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.routerecipt.project.mapper.ReceiptMapper;

@Service
public class ReceiptServiceImp implements ReceiptService {
	
	@Autowired
	private ReceiptMapper receiptMapper;
}
