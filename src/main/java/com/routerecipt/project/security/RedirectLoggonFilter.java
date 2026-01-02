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

@Component
@Order(1)
public class RedirectLoggonFilter extends OncePerRequestFilter {
	
	@Override
	protected void doFilterInternal(HttpServletRequest request,
	                                HttpServletResponse response,
	                                FilterChain filterChain)
	        throws ServletException, IOException {

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
