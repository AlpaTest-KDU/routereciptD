package com.routerecipt.project.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MainController {
	
	// Branch Test
	// 메인 화면
	@GetMapping("/")
	public String mainPage() {
		return "index";
	}
	
	// 로그인 화면
	@GetMapping("user/userLoginPage")
	public String userLoginPage() {
		return "user/userLoginPage";
	}
	
	// 아이디, 비밀번호 찾기 화면
	@GetMapping("user/userFindPage")
	public String userFindPage() {
		return "user/userFindPage";
	}
	
	// 회원가입 화면
	@GetMapping("user/userSignUpPage")
	public String userSignUpPage() {
		return "user/userSignUpPage";
	}
	
	// 마이페이지 화면
	@GetMapping("user/userInfoShowPage")
	public String userInfoShowPage() {
		return "user/userInfoShowPage";
	}
	
	// 공지사항 화면
	@GetMapping("notice/noticePage")
	public String noticePage() {
		return "notice/noticePage";
	}

	// 회원정보수정 화면
	@GetMapping("user/userInfoUpdatePage")
	public String getMethodName() {
		return "user/userInfoUpdate";
	}
	
	// 지출분석 화면
	@GetMapping("user/analysisPage")
	public String analysisPage() {
		return "user/analysisPage";
	}
	
	// 영수증 등록 화면
	@GetMapping("recepit/receiptRegisterPage")
	public String receiptRegisterPage() {
		return "receipt/receiptRegisterPage";
	}
	
	// 챗봇 화면
	@GetMapping("chatbot/chatBotPage")
	public String chatBotPage() {
		return "chatbot/chatBotPage";
	}
	
	// 운영자 관리 화면 (관리자만 접근 가능)
	@GetMapping("admin/adminPage")
	@PreAuthorize("hasRole('ADMIN')")
	public String adminPage() {
		return "admin/adminPage";
	}
	
	// 유저 영수증 관리 화면 (관리자만 접근 가능)
	@GetMapping("admin/userReceiptInfoPage")
	@PreAuthorize("hasRole('ADMIN')")
	public String userReceiptInfoPage() {
		return "admin/userReceiptInfoPage";
	}
}