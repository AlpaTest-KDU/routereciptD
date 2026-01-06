package com.routerecipt.project.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;


/**
 * 커스텀 에러 페이지 컨트롤러
 *
 * 역할:
 *  - 접근 권한이 없는 경우(403 Forbidden)
 *  - 기본 에러 페이지 대신 커스텀 화면 반환
 *
 * 연동 위치:
 *  - Spring Security accessDeniedPage("/error/403")
 */
@Controller
public class CustomErrorController {

	/**
     * 403 Forbidden 에러 페이지
     *
     * @return error/403 뷰 템플릿
     */
    @GetMapping("/error/403")
    public String accessDenied() {
    	
    	// templates/error/403.html 이동
        return "error/403";
    }
}
