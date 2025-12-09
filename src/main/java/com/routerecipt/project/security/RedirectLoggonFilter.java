package com.routerecipt.project.security;

import java.io.IOException;

import org.springframework.core.annotation.Order;
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
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		
		if ("/login".equals(request.getRequestURI()) && request.getMethod().equalsIgnoreCase("POST")) {
			
			String username = request.getParameter("u_id");
			String password = request.getParameter("u_pw");
			
			System.out.println("====로그인 요청 발생====");
			System.out.println("입력한 ID" + username);
			System.out.println("입력한 PW" + (password != null ? "*".repeat(password.length()) : null));
			System.out.println("===================");
			
		}
		
		filterChain.doFilter(request, response);
		
		int status = response.getStatus();
		if (status == HttpServletResponse.SC_FOUND) {
			
		}
		
	}
}
