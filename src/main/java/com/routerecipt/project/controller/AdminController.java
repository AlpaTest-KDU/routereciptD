package com.routerecipt.project.controller;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.routerecipt.project.dto.Userdto;
import com.routerecipt.project.service.UserServiceImp;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

	private final UserServiceImp userServiceImp;

	// 사용자 탈퇴 시키기
	@PostMapping("/userInfoDelete")
	public String userInfoDelete(@RequestParam(value="u_id") String u_id) {
		userServiceImp.UserInfoDelete(u_id);
		return "admin/adminPage";
	}
	
	// id로 사용자 검색
	@GetMapping("/userInfoSelect")
	public String userInfoSelect(Model model, @RequestParam(value = "u_id") String u_id) {
		Userdto user = userServiceImp.UserSelectById(u_id);
		model.addAttribute("userInfo", user);
		return "admin/adminPage";
	}
}
