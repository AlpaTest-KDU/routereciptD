package com.routerecipt.project.controller;

import java.util.List;

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
public class AdminController {
	
	@Autowired
	private UserServiceImp userServiceImp;
	
	
	// 사용자 탈퇴 시키기
	@PostMapping("/userInfoDelete")
	public String userInfoDelete(@RequestParam(value="u_id") String u_id) {
		userServiceImp.UserInfoDelete(u_id);
		return "admin/adminPage";
	}
	
	@GetMapping("/userReceiptInfoSelect")
	public String userReceiptInfoSelect(
				@RequestParam(value = "year") int year,
				@RequestParam(value = "month") int month) {
		return "userReceiptInfoPage";
	}
	
	// 이름으로 사용자 검색
	@GetMapping("/userInfoSelect")
	public String userInfoSelect(Model model, @RequestParam(value = "u_name") String userName) {
		List<Userdto> user = userServiceImp.selectUserByName(userName);
		model.addAttribute("userList", user);
		return "admin/adminPage";
	}
	

	// 마이페이지 화면 (관리자가 id로 검색)
	@GetMapping("/userInfoShowPage/{u_id}")
	public String userInfoShowPage(@PathVariable(value="u_id") String u_id, Authentication authentication, Model model) {
		Userdto user = userServiceImp.selectUserById(u_id);

    	model.addAttribute("u_id", user.getU_id());
    	model.addAttribute("u_name", user.getU_name());
    	model.addAttribute("u_email", user.getU_email());
    	model.addAttribute("u_birthday", user.getU_birthday());
    	model.addAttribute("gender", user.getGender());
		return "user/userInfoShowPage";
	}
}
