package com.routerecipt.project.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import com.routerecipt.project.service.ReceiptServiceImp;
import com.routerecipt.project.service.UserServiceImp;

@Controller
public class AdminController {
	
	@Autowired
	private UserServiceImp userServiceImp;
	
	@Autowired
	private ReceiptServiceImp receiptServiceImp;
	
	@DeleteMapping("adminPage/userInfoDelete/{id}")
	public String userInfoDelete(@PathVariable(value="id") String id) {
		return "adminPage";
	}
	
	@GetMapping("/userReceiptInfoPage/userReceiptInfoSelect")
	public String userReceiptInfoSelect(
				@RequestParam(value = "year") int year,
				@RequestParam(value = "month") int month) {
		return "userReceiptInfoPage";
	}
	
	@GetMapping("/userReceiptInfoPage/userInfoSelect")
	public String userInfoSelect(@RequestParam(value = "userName") String userName) {
		return "userReceiptInfoPage";
	}
}
