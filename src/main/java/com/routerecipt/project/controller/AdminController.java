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

import com.routerecipt.project.dto.UserDTO;
import com.routerecipt.project.service.UserServiceImp;


/**
 * 관리자(Admin) 전용 컨트롤러
 *
 * 역할:
 *  - 관리자 페이지 요청 처리
 *  - 사용자 정보 조회 / 삭제 기능 제공
 *
 * URL Prefix:
 *  - /admin/**
 */
@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {
	
	
	/** 사용자 관리 비즈니스 로직 서비스 */
	private final UserServiceImp userServiceImp;

	/**
     * 사용자 삭제 처리
     *
     * @param u_id 삭제할 사용자 ID
     * @return 관리자 페이지 View
     */
	@PostMapping("/userInfoDelete")
	public String userInfoDelete(@RequestParam(value="u_id") String u_id) {
		 // 사용자 삭제 (Service에 위임)
		userServiceImp.UserInfoDelete(u_id);
		
		// 삭제 후 관리자 페이지로 이동
		return "admin/adminPage";
	}
	
	/**
     * 사용자 ID로 사용자 정보 조회
     *
     * @param model View에 전달할 데이터 컨테이너
     * @param u_id  조회할 사용자 ID
     * @return 관리자 페이지 View
     */
	@GetMapping("/userInfoSelect")
	public String userInfoSelect(Model model, @RequestParam(value = "u_id") String u_id) {
		// 사용자 정보 조회
		UserDTO user = userServiceImp.UserSelectById(u_id);
		
		// 조회 결과를 View에서 사용할 수 있도록 Model에 담기
		model.addAttribute("userInfo", user);
		
		// 관리자 페이지 반환
		return "admin/adminPage";
	}
}
