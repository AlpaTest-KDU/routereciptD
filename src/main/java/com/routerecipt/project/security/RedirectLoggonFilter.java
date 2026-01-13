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
@Order(1)
public class RedirectLoggonFilter extends OncePerRequestFilter {

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();

        // ⭐ 무중단 배포 / 인프라 경로는 아예 필터 적용 안 함
        return uri.startsWith("/health")
            || uri.startsWith("/ai/")
            || uri.startsWith("/css/")
            || uri.startsWith("/js/")
            || uri.startsWith("/img/")
            || uri.equals("/favicon.ico");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String uri = request.getRequestURI();

        // 여기부터는 "필터 적용 대상 요청만" 들어옴
        Authentication auth =
                SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.isAuthenticated()
            && auth.getPrincipal() instanceof LoginDetails
            && uri.equals("/user/userLoginPage")) {

            response.sendRedirect("/");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
