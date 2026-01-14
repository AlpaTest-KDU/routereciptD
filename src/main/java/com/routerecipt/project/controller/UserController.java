package com.routerecipt.project.controller;

import java.security.Principal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.routerecipt.project.service.ReceiptQueryService;
import com.routerecipt.project.service.ReceiptQueryServiceImp;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.routerecipt.project.dto.ReceiptAnalysisStatsdto;
import com.routerecipt.project.dto.ReceiptDTO;
import com.routerecipt.project.dto.Role;
import com.routerecipt.project.dto.UserDTO;
import com.routerecipt.project.mapper.ReceiptMapper;
import com.routerecipt.project.receipt.ReceiptResultService;
import com.routerecipt.project.redis.BloomFilter.RedisBloomService;
import com.routerecipt.project.security.LoginDetails;
import com.routerecipt.project.service.ReceiptApplicationServiceImp;
import com.routerecipt.project.service.UserServiceImp;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

	private final ReceiptResultService receiptResultService;
	private final ReceiptQueryServiceImp receiptQueryServiceImp;
	private final UserServiceImp userServiceImp;
	private final PasswordEncoder passwordEncoder;
	private final RedisBloomService bloomService;

	// 회원가입 기능
	@PostMapping("/userSignUp")
	public String userSignUp(@Valid @ModelAttribute("userDto") UserDTO u, BindingResult bindingResult, Model model,
							 @RequestParam(name = "emailDomain") String emailDomain,
							 @RequestParam(name = "emailDomainCustom", required = false) String emailDomainCustom) {
		if (bindingResult.hasErrors()) {
			return "user/userSignUpPage";
		}

		String domain = emailDomain.equals("etc") ? emailDomainCustom : emailDomain;
		u.setU_email(u.getU_email() + "@" + domain);

		// 주입받은 PasswordEncoder 인스턴스로 암호화
		u.setU_pw(passwordEncoder.encode(u.getU_pw()));
		u.setRole(Role.ROLE_USER);
		
		try {
			// 회원가입 처리
			userServiceImp.UserSignUp(u);
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
			bloomService.register(u.getU_id());
		} catch (Exception e) {
			log.warn("Bloom Filter 등록 실패: {}",u.getU_id(),e);
		}
		
		return "redirect:/";
	}

	// 회원정보수정 화면
	@GetMapping("/userInfoUpdatePage")
	public String userInfoUpdatePage(Authentication authentication, Model model) {
		LoginDetails principal = (LoginDetails) authentication.getPrincipal();
		String loginUserId = principal.getUser().getU_id();
		
		UserDTO user = userServiceImp.loadUserByUsername(loginUserId);

    	model.addAttribute("u_id", user.getU_id());
    	model.addAttribute("u_name", user.getU_name());
    	model.addAttribute("u_email", user.getU_email());
    	model.addAttribute("u_birthday", user.getU_birthday());
    	model.addAttribute("gender", user.getGender());
		return "user/userInfoUpdate";
	}
	
	
	// 정보 수정 기능
	@PostMapping("/userInfoUpdate")
	public String userInfoUpdate(UserDTO u, Authentication authentication) {
		LoginDetails principal = (LoginDetails) authentication.getPrincipal();
	    String loginUserId = principal.getUser().getU_id();
	    
	    u.setU_id(loginUserId);
		
		// 1. DB업데이트
		userServiceImp.UserInfoUpdate(u);

		return "redirect:/user/userInfoShowPage";
	}
	
	// 회원 삭제 기능
	@PostMapping("/userInfoDelete")
	public String userInfoDelete(Authentication authentication, HttpServletRequest req) throws ServletException {
		
		LoginDetails loginDetails = (LoginDetails) authentication.getPrincipal();
		String loginUserId = loginDetails.getUser().getU_id();
		
		userServiceImp.UserInfoDelete(loginUserId);
		
		req.logout();
		return "redirect:/";
	}
	
	// 중복검사
	@PostMapping("/checkUserId")
	@ResponseBody
	public boolean checkUserId(@RequestParam String userId) {
		return userServiceImp.checkDuplicateUserId(userId); // true면 중복
	}

	// 마이페이지 -> 월별 지출로
	 @GetMapping("/analysisMonthlyPage")
	    public String analysisMonthlyPage(Authentication authentication, Model model) {

	        LoginDetails loginDetails = (LoginDetails) authentication.getPrincipal();
	        String uId = loginDetails.getUser().getU_id();

	        ReceiptAnalysisStatsdto stats = receiptResultService.getAnalysisMonthlyPageStats(uId);

	        model.addAttribute("dailyData", stats.getDailyData());
	        model.addAttribute("weeklyData", stats.getWeeklyData());
	        model.addAttribute("monthlyData", stats.getMonthlyData());

	        return "user/monthlyanalysisPage";
	    }
}
