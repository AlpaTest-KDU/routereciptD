package com.routerecipt.project.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.routerecipt.project.dto.UserDTO;
import com.routerecipt.project.entity.User;
import com.routerecipt.project.service.UserServiceImp;

@Controller
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserfindController {

	private final UserServiceImp userServiceImp;

	// 아이디 찾기 기능
	// 파라미터 = email
	@PostMapping("/userFindId")
	public String userFindId(Model model, @RequestParam(value="u_email") String u_email) {
//		UserDTO user = userServiceImp.UserfindID(u_email);
//		if (user != null) {
//			model.addAttribute("foundId", user.getU_id());
//		} else {
//			model.addAttribute("idError", "존재하지 않는 회원입니다.");
//		}
//		return "user/userFindIdPage";
		String foundId = userServiceImp.UserfindID(u_email);
		if (foundId != null) {
			model.addAttribute("foundId", foundId);
		}else {
			model.addAttribute("idError", "존재하지 않는 회원입니다.");
		}
		return "user/userFindIdPage";
		
	}
	
	// 비밀번호 변경 전 아이디, 이메일 체크
	// 파라미터 = u_id, u_email
	@PostMapping("/userCheckId")
	public String userCheckId(Model model, @RequestParam(value="u_id") String u_id, @RequestParam(value="u_email") String u_email) {
//		int result = userServiceImp.UserCheckID(u_id, u_email);
//
//		if (result == 1) {
//			UserDTO user = userServiceImp.UserFindID(u_email);
//			model.addAttribute("verifiedId", user.getU_id());
//		} else {
//			model.addAttribute("idError", "존재하지 않는 회원입니다.");
//		}
//		// redirect: 사용시 Model 데이터가 사라지기 때문에 사용 X
//		return "user/userResetPwPage";
		
		boolean result = userServiceImp.UserCheckID(u_id, u_email);
		
		if(result) {
			String verifiedId = userServiceImp.UserfindID(u_email);
			model.addAttribute("verifiedId", verifiedId);
		} else {
			model.addAttribute("idError", "존재하지 않는 회원입니다.");
		}
		return "user/userResetPwPage";
	}

	// 비밀번호 찾기 기능
	// 파라미터 = Userdto
	@PostMapping("/userUpdatePw")
	public String userUpdatePw(@ModelAttribute UserDTO user) {
		userServiceImp.UserUpdatePW(user);
		return "redirect:/user/userLoginPage";
	}
}
