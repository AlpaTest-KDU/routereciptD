package com.routerecipt.project.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.routerecipt.project.service.NoticeServiceImp;

@Controller
@RequestMapping("/noticePage")
public class NoticeController {
	
	@Autowired
	private NoticeServiceImp noticeServiceImp;
	
	@PostMapping("/noticeRegister")
	public String noticeRegister() {
		return "noticePage";
	}
	
	@GetMapping("/noticeShow/{noticeTitle}")
	public String noticeShow(@RequestParam("noticeTitle") String noticeTitle) {
		return "noticePage";
	}
}	
