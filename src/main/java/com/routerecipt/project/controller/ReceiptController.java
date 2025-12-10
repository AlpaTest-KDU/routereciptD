package com.routerecipt.project.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import com.routerecipt.project.service.ReceiptServiceImp;

@Controller
public class ReceiptController {
	
	@Autowired
	private ReceiptServiceImp receptServiceImp;
	
	// 영수증 등록 기능
	@PostMapping("/receiptRegisterPage/ReceiptRegister")
	public String receiptRegister() {
		return "receiptRegisterPage";
	}
	
	// 영수증 삭제 기능
	@DeleteMapping("/userInfoShowPage/receiptInfoDelete/{receiptNo}")
	public String receiptInfoDelete(@PathVariable(value="receiptNo") Long receiptNo) {
		return "userInfoShowPage";
	}
	
	@PostMapping("/userInfoShowPage/receiptInfoShow")
	public String receiptInfoShow() {
		return "userInfoShowPage";
	}
}
