package com.routerecipt.project.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.routerecipt.project.dto.Role;
import com.routerecipt.project.dto.Userdto;
import com.routerecipt.project.service.UserServiceImp;

import jakarta.validation.Valid;

@Service
public class UserController {
	
	@Autowired
	private UserServiceImp userServiceImp;
	
	@Autowired
	private PasswordEncoder passwordEncoder;
	// 아이디,비번 찾기 기능
	// 파라미터 명 = email
	@GetMapping("/userFindPage/userfind")
	public String userfind(@RequestParam(value="email") String u_email) {
		userServiceImp.UserFind(u_email);
		return "index";
	}
	
	// 회원가입 기능
	@PostMapping("/userSignUpPage/userSignUp")
	public String userSignUp(@Valid @ModelAttribute("userdto") Userdto u, Errors errors, Model model) {
		// 주입받은 PasswordEncoder 인스턴스로 암호화
		u.setU_pw(passwordEncoder.encode(u.getU_pw()));
		
		u.setRole(Role.ROLE_USER);
		
		if (errors.hasErrors()) {
			// 회원가입 실패시 입력 데이터 값을 유지
			model.addAttribute("userdto",u);
		}
		try {
			userServiceImp.UserSignUp(u);
			
		} catch (Exception e) {
			model.addAttribute("dupilcateError", e.getMessage());
			model.addAttribute("userdto", u);
			
			return "/userSignUpPage/userSignUp";
		}
		
		return "index";
	}
	
	// 정보 수정 기능
	@PostMapping("/userInfoShowPage/userInfoUpdate")
	public String userInfoUpdate(Userdto u) {
		userServiceImp.UserInfoUpdate(u);
		return "userInfoShowPage";
	}
	
	// 회원 삭제 기능
	@DeleteMapping("/userInfoShowPage/userInfoDelete")
	public String userInfoDelete(Userdto u) {
		userServiceImp.UserInfoDelete(u);
		return "index";
	}
}
