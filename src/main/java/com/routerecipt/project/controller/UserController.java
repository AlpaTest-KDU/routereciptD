package com.routerecipt.project.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.routerecipt.project.dto.Role;
import com.routerecipt.project.dto.Userdto;
import com.routerecipt.project.service.UserServiceImp;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/user")
public class UserController {
	
	@Autowired
	private UserServiceImp userServiceImp;
	
	@Autowired
	private PasswordEncoder passwordEncoder;
	
	@GetMapping("/user/userLoginPage")
	public String userLoginPage() {
		return "index";
	}
	// 아이디,비번 찾기 기능
	// 파라미터 명 = email
	@GetMapping("/userfind")
	public String userfind(@RequestParam(value="email") String u_email) {
		userServiceImp.UserFind(u_email);
		return "index";
	}
	
	// 회원가입 기능
	@PostMapping("/userSignUp")
	public String userSignUp(@Valid @ModelAttribute("userdto") Userdto u, Model model,@RequestParam(name = "emailDomain") String emailDomain,@RequestParam(name = "emailDomainCustom",required = false) String emailDomainCustom) {
		String domain = emailDomain.equals("etc") ? emailDomainCustom : emailDomain;
		u.setU_email(u.getU_email() + "@" + domain);
		
		System.out.println("===== userSignUp 호출됨 =====");
		System.out.println("u_id=" + u.getU_id());
		System.out.println("u_pw=" + u.getU_pw());
		System.out.println("u_name=" + u.getU_name());
		System.out.println("u_email=" + u.getU_email());
		System.out.println("u_birthday=" + u.getU_birthday());
		System.out.println("gender=" + u.getGender());
		System.out.println("role=" + u.getRole());
		
		if (u.getU_birthday() == null) {
			model.addAttribute("birthdayError", "생년월일을 입력하세요");
			return "user/userSignUpPage";
		}
		

		// 주입받은 PasswordEncoder 인스턴스로 암호화
		u.setU_pw(passwordEncoder.encode(u.getU_pw()));
		
		u.setRole(Role.ROLE_USER);
		
		userServiceImp.UserSignUp(u);
//		try {
//			
//		} catch (Exception e) {
//			model.addAttribute("dupilcateError", e.getMessage());
//			model.addAttribute("userdto", u);
//			
//			return "user/userSignUpPage";
//		}
		
		return "index";
	}
	
	// 정보 수정 기능
	@PostMapping("/userInfoUpdate")
	public String userInfoUpdate(Userdto u) {
		userServiceImp.UserInfoUpdate(u);
		return "userInfoShowPage";
	}
	
	// 회원 삭제 기능
	@DeleteMapping("/userInfoDelete")
	public String userInfoDelete(Userdto u) {
		userServiceImp.UserInfoDelete(u);
		return "index";
	}
}
