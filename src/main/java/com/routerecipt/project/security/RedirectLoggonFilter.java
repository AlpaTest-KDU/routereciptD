package com.routerecipt.project.security;

import java.io.IOException;

import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;


/**
 * 로그인 상태에 따라 페이지 접근을 제어하는 필터
 * - 이미 로그인한 사용자가 로그인 페이지로 접근하면 메인으로 리다이렉트
 * - 특정 경로는 인증 여부와 상관없이 통과
 */
@Component
@Order(1)	// 필터 실행 우선순위 (숫자가 작을수록 먼저 실행)
public class RedirectLoggonFilter extends OncePerRequestFilter {
	
	@Override
	protected void doFilterInternal(HttpServletRequest request,
	                                HttpServletResponse response,
	                                FilterChain filterChain)
	        throws ServletException, IOException {

		// 현재 요청 URI
	    String uri = request.getRequestURI();
	    
	    // ✅ AI / API 요청은 무조건 통과
	    if (uri.startsWith("/ai/")) {
	        filterChain.doFilter(request, response);
	        return;
	    }

	    // 🔒 비로그인 접근 허용 페이지
	    if (
	        uri.equals("/") ||
	        uri.startsWith("/css") ||
	        uri.startsWith("/js") ||
	        uri.equals("/user/userLoginPage") ||
	        uri.equals("/user/userSignUpPage") ||
	        uri.equals("/user/userFindIdPage") ||
	        uri.equals("/user/userResetPwPage") ||
	        uri.equals("/user/analysisPage")
	    ) {
	        filterChain.doFilter(request, response);
	        return;
	    }
	    
	    
	    // 현재 인증(로그인) 정보 조회
	    Authentication auth = SecurityContextHolder.getContext().getAuthentication();

	    // 로그인 상태인데 로그인 페이지 접근하면 메인으로
	    if (auth != null && auth.isAuthenticated()
	        && auth.getPrincipal() instanceof LoginDetails
	        && uri.equals("/user/userLoginPage")) {

	        response.sendRedirect("/");
	        return;
	    }
	    
	    
	    filterChain.doFilter(request, response);
	}
}
