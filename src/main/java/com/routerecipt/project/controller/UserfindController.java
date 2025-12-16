package com.routerecipt.project.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.routerecipt.project.dto.Userdto;
import com.routerecipt.project.service.UserServiceImp;

@Controller
@RequestMapping("/user")
public class UserfindController {

	@Autowired
	private UserServiceImp userServiceImp;

	// 아이디 찾기 기능
	// 파라미터 = email
	@PostMapping("/userFindId")
	public String userFindId(Model model, @RequestParam(value="email") String u_email) {
		Userdto user = userServiceImp.UserFindID(u_email);
		if (user != null) {
			model.addAttribute("foundId", user.getU_id());
		} else {
			model.addAttribute("idError", "존재하지 않는 회원입니다.");
		}
		return "user/userFindIdPage";
	}
	
	// 비밀번호 변경 전 아이디, 이메일 체크
	// 파라미터 = u_id, u_email
	@PostMapping("/userCheckId")
	public String userCheckId(Model model, @RequestParam(value="u_id") String u_id, @RequestParam(value="u_email") String u_email) {
		Userdto user = userServiceImp.UserCheckID(u_id, u_email);
		if (user != null) {
			model.addAttribute("verifiedId", user.getU_id());
		} else {
			model.addAttribute("idError", "존재하지 않는 회원입니다.");
		}
		return "redirect:/user/ResetPwPage";
	}

	// 비밀번호 찾기 기능
	// 파라미터 = Userdto
	@PostMapping("/userUpdatePw")
	public String userUpdatePw(@ModelAttribute Userdto user) {
		userServiceImp.UserUpdatePW(user);
		return "redirect:/user/userLoginPage";
	}
}
