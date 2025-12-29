package com.routerecipt.project.controller;

import java.security.Principal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;


import com.routerecipt.project.dto.Role;
import com.routerecipt.project.dto.Userdto;
import com.routerecipt.project.ocr.ServiceIMP;
import com.routerecipt.project.redis.BloomFilter.RedisBloomService;
import com.routerecipt.project.security.LoginDetails;
import com.routerecipt.project.service.UserServiceImp;
import com.routerecipt.project.dto.ReceiptDTO;
import com.routerecipt.project.mapper.ReceiptMapper;
import com.routerecipt.project.mapper.UserMapper;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequestMapping("/user")
public class UserController {
	
	@Autowired
	private UserServiceImp userServiceImp;
	
	@Autowired
	private PasswordEncoder passwordEncoder;
	
	@Autowired
	private RedisBloomService bloomService;
	
	@Autowired
    private ServiceIMP usi;
	
	@Autowired
    private UserMapper um;
	
	@Autowired
	private ReceiptMapper rm;
	
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
			log.warn("Bloom Filter 등록 실패: {}",u.getU_id(),e);
		}
		
		return "redirect:/";
	}
	
	@GetMapping("/mypage")
    public String mypage(
            @RequestParam(value = "yearMonth", required = false) String yearMonth,
            @RequestParam(value = "day", required = false) Integer day,
            Principal principal,
            Model model) {

        // 1️⃣ 로그인 체크
        if (principal == null) {
            return "redirect:/";
        }
        String r_u = principal.getName();

        // 2️⃣ yearMonth 기본값
        if (yearMonth == null || yearMonth.isBlank()) {
            yearMonth = YearMonth.now().toString(); // yyyy-MM
        }

        YearMonth ym = YearMonth.parse(yearMonth);
        String prevYearMonth = ym.minusMonths(1).toString();

        log.warn("mypage: yearMonth={}, user={}", yearMonth, r_u);

        // 3️⃣ 이번 달 영수증 조회
        List<ReceiptDTO> receipts =
                rm.getSavedReceiptsDate(r_u, prevYearMonth);

        // 4️⃣ menuMap 생성 (🔥 빠져 있던 핵심)
        Map<String, List<String>> menuMap =
                usi.buildMenuMap(receipts);

        // 5️⃣ 월 합계
        int monthTotal = receipts.stream()
                .mapToInt(ReceiptDTO::getR_price)
                .sum();

        // 6️⃣ 지난 달 합계
        List<ReceiptDTO> prevReceipts =
                usi.getSavedReceiptsDate(r_u, prevYearMonth);
        log.warn(">>> [CTRL] receipts.size() = {}", receipts.size());

        int prevMonthTotal = prevReceipts.stream()
                .mapToInt(ReceiptDTO::getR_price)
                .sum();

        // 7️⃣ 날짜별 그룹핑 (상세 보기용)
        Map<Integer, List<ReceiptDTO>> grouped = new HashMap<>();
        for (ReceiptDTO r : receipts) {
            if (r.getR_date() == null) continue;
            int d = r.getR_date().getDayOfMonth();
            grouped.computeIfAbsent(d, k -> new ArrayList<>()).add(r);
        }

        // 8️⃣ 캘린더
        List<Integer> calendar = usi.buildCalendar(yearMonth);

        // 9️⃣ 모델에 전부 세팅 (🔥 중요)
        model.addAttribute("calendar", calendar);
        model.addAttribute("menuMap", menuMap);
        model.addAttribute("yearMonth", yearMonth);

        model.addAttribute("monthTotal", monthTotal);
        model.addAttribute("prevMonthTotal", prevMonthTotal);
        model.addAttribute("diffTotal", monthTotal - prevMonthTotal);

        // 🔟 날짜 선택 시 상세 데이터
        if (day != null) {
            model.addAttribute("selectedDay", day);
            model.addAttribute(
                    "selectedReceipts",
                    grouped.getOrDefault(day, Collections.emptyList())
            );
        }

        return "info";
    }
	
	// 회원정보수정 화면
	@GetMapping("/userInfoUpdatePage")
	public String userInfoUpdatePage(Authentication authentication, Model model) {
		userServiceImp.UserInfoShow();
		LoginDetails principal = (LoginDetails) authentication.getPrincipal();
		String loginUserId = principal.getUser().getU_id();
		
		Userdto user = userServiceImp.loadUserByUsername(loginUserId);
		
		
    	model.addAttribute("u_id", user.getU_id());
    	model.addAttribute("u_name", user.getU_name());
    	model.addAttribute("u_email", user.getU_email());
    	model.addAttribute("u_birthday", user.getU_birthday());
    	model.addAttribute("gender", user.getGender());
		return "user/userInfoUpdate";
	}
	
	
	// 정보 수정 기능
	@PostMapping("/userInfoUpdate")
	public String userInfoUpdate(Userdto u, Authentication authentication) {
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
}
