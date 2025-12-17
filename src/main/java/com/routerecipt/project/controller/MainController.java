package com.routerecipt.project.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.routerecipt.project.dto.Noticedto;
import com.routerecipt.project.dto.Userdto;
import com.routerecipt.project.security.LoginDetails;
import com.routerecipt.project.service.NoticeServiceImp;

@Controller
public class MainController {
	
	@Autowired
	private NoticeServiceImp noticeServiceImp;
	// Branch Test
	// 메인 화면
	@GetMapping("/")
	public String mainPage() {
		return "index";
	}
	
	// 로그인 화면
	@GetMapping("/user/userLoginPage")
	public String userLoginPage() {
		return "user/userLoginPage";
	}
	
	// 아이디 찾기 화면
	@GetMapping("/user/userFindIdPage")
	public String userFindIdPage() {
		return "user/userFindIdPage";
	}

	// 비밀번호 재설정 화면
	@GetMapping("/user/userResetPwPage")
	public String userResetPwPage() {
		return "user/userResetPwPage";
	}
	
	// 회원가입 화면
	@GetMapping("/user/userSignUpPage")
	public String userSignUpPage() {
		return "user/userSignUpPage";
	}
	
	// 마이페이지 화면
	@GetMapping("/user/userInfoShowPage")
	public String userInfoShowPage(Authentication authentication, Model model) {
		LoginDetails loginDetails = (LoginDetails) authentication.getPrincipal();
		Userdto user = loginDetails.getUser();

    	model.addAttribute("u_id", user.getU_id());
    	model.addAttribute("u_name", user.getU_name());
    	model.addAttribute("u_email", user.getU_email());
    	model.addAttribute("u_birthday", user.getU_birthday());
    	model.addAttribute("gender", user.getGender());
		return "user/userInfoShowPage";
	}
		
	// 지출분석 화면
	@GetMapping("/user/analysisPage")
	public String analysisPage() {
		return "user/analysisPage";
	}
	
	// 영수증 등록 화면
	@GetMapping("/recepit/receiptRegisterPage")
	public String receiptRegisterPage() {
		return "receipt/receiptRegisterPage";
	}
	
	// 챗봇 화면
	@GetMapping("/chatbot/chatBotPage")
	public String chatBotPage() {
		return "chatbot/chatBotPage";
	}
	
	// 운영자 관리 화면 (관리자만 접근 가능)
	@GetMapping("/admin/adminPage")
	@PreAuthorize("hasRole('ADMIN')")
	public String adminPage() {
		return "admin/adminPage";
	}
	
	// 유저 영수증 관리 화면 (관리자만 접근 가능)
	@GetMapping("/admin/userReceiptInfoPage")
	@PreAuthorize("hasRole('ADMIN')")
	public String userReceiptInfoPage() {
		return "admin/userReceiptInfoPage";
	}
}