package com.routerecipt.project.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.routerecipt.project.service.UserServiceImp;

@Service
public class UserController {
	
	@Autowired
	private UserServiceImp userServiceImp;
	
	// 아이디,비번 찾기 기능
	// 파라미터 명 = email
	@GetMapping("/userFindPage/userfind")
	public String userfind(@RequestParam(value="email") String email) {
		return "index";
	}
	
	// 회원가입 기능
	@PostMapping("/userSignUpPage/userSignUp")
	public String userSignUp() {
		return "index";
	}
	
	// 정보 수정 기능
	@PostMapping("/userInfoShowPage/userInfoUpdate")
	public String userInfoUpdate() {
		return "userInfoShowPage";
	}
	
	// 회원 삭제 기능
	@DeleteMapping("/userInfoShowPage/userInfoDelete/{id}")
	public String userInfoDelete(@PathVariable(value="id") String id) {
		return "index";
	}
}
