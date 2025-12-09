package com.routerecipt.project.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MainController {
	
	// 메인 화면
	@GetMapping("/")
	public String mainPage() {
		return "index";
	}
	
	// 로그인 화면
	@GetMapping("/userLoginPage")
	public String userLoginPage() {
		return "userLoginPage";
	}
	
	// 아이디, 비밀번호 찾기 화면
	@GetMapping("/userFindPage")
	public String userFindPage() {
		return "userFindPage";
	}
	
	// 회원가입 화면
	@GetMapping("/userSignUpPage")
	public String userSignUpPage() {
		return "userSignUpPage";
	}
	
	// 마이페이지 화면
	@GetMapping("/userInfoShowPage")
	public String userInfoShowPage() {
		return "userInfoShowPage";
	}
	
	// 공지사항 화면
	@GetMapping("/noticePage")
	public String noticePage() {
		return "noticePage";
	}
	
	// 지출분석 화면
	@GetMapping("/analysisPage")
	public String analysisPage() {
		return "analysisPage";
	}
	
	// 영수증 등록 화면
	@GetMapping("/receiptRegisterPage")
	public String receiptRegisterPage() {
		return "receiptRegisterPage";
	}
	
	// 챗봇 화면
	@GetMapping("/chatBotPage")
	public String chatBotPage() {
		return "chatBotPage";
	}
	
	// 운영자 관리 화면 (관리자만 접근 가능)
	@GetMapping("/adminPage")
	@PreAuthorize("hasRole('ADMIN')")
	public String adminPage() {
		return "adminPage";
	}
	
	// 유저 영수증 관리 화면 (관리자만 접근 가능)
	@GetMapping("/userReceiptInfoPage")
	@PreAuthorize("hasRole('ADMIN')")
	public String userReceiptInfoPage() {
		return "userReceiptInfoPage";
	}
}