package com.routerecipt.project.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;


import com.routerecipt.project.dto.Role;
import com.routerecipt.project.dto.Userdto;
import com.routerecipt.project.redis.BloomFilter.RedisBloomService;
import com.routerecipt.project.security.LoginDetails;
import com.routerecipt.project.service.UserServiceImp;

import jakarta.validation.Valid;


@Controller
@RequestMapping("/user")
public class UserController {
	
	@Autowired
	private UserServiceImp userServiceImp;
	
	@Autowired
	private PasswordEncoder passwordEncoder;
	
	@Autowired
	private RedisBloomService bloomService;
	
	// 아이디 찾기 기능
	// 파라미터 = email
	@PostMapping("/userFindId")
	public String userFindId(@RequestParam(value="email") String u_email) {
		userServiceImp.UserFindID(u_email);
		return "userFindPage";
	}

	// 비밀번호 찾기 기능
	// 파라미터 = Userdto
	@PostMapping("/userFindPw")
	public String userUpdatePw(@ModelAttribute Userdto user) {
		userServiceImp.UserUpdatePW(user);
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
		
		try {
			// 회원가입 처리
			userServiceImp.UserSignUp(u);
			
			// Bloom Filter에 ID 추가
			bloomService.addUserId(u.getU_id());
			
		} catch (DataIntegrityViolationException e) {
			model.addAttribute("dupilcateError", "이미 사용 중인 아이디입니다.");
			model.addAttribute("message", "회원가입 실패: " + e.getMessage());
			model.addAttribute("userdto", u);
			
			return "user/userSignUpPage";
		} catch (Exception e) {
			model.addAttribute("message", "회원가입 중 오류가 발생했습니다.");
			model.addAttribute("userdto",u);
			return "user/userSignUpPage";
		}
		
		// bloom Filter 등록 (실패해도 회원가입은 성공)
		try {
			bloomService.addUserId(u.getU_id());
		} catch (Exception e) {
		}
		
		return "index";
	}
	
	// 회원정보수정 화면
	@GetMapping("/userInfoUpdatePage")
	public String userInfoUpdatePage(Authentication authentication, Model model) {
		userServiceImp.UserInfoShow();
		System.out.println("🔥 userInfoUpdatePage CONTROLLER HIT 🔥");
		LoginDetails loginDetails = (LoginDetails) authentication.getPrincipal();
		Userdto user = loginDetails.getUser();

    	model.addAttribute("u_id", user.getU_id());
    	model.addAttribute("u_name", user.getU_name());
    	model.addAttribute("u_email", user.getU_email());
    	model.addAttribute("u_birthday", user.getU_birthday());
    	model.addAttribute("gender", user.getGender());
		return "user/userInfoUpdate";
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
	
	// 중복검사
	@PostMapping("/checkUserId")
	@ResponseBody
	public boolean checkUserId(@RequestParam String userId) {
		return userServiceImp.checkDuplicateUserId(userId); // true면 중복
	}
}
