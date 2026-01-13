package com.routerecipt.project.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 전체 보안 설정 클래스
 * 인증, 인가, 로그인, 로그아웃, 필터 순서를 정의한다.
 */
@Configuration
@EnableWebSecurity(debug = true)
public class SecurityConfig {
	
	// Security Filter Chain 설정
	@Bean
	@Order(1)
	public SecurityFilterChain filterChan(HttpSecurity http, RedirectLoggonFilter redirectLoggonFilter) throws Exception {
		http
			// CSRF 설정 (AI API는 예외 처리)
			 .csrf(csrf -> csrf	
            .ignoringRequestMatchers("/ai/**")
            .ignoringRequestMatchers("/receipt/uploadReceipt")
        	)
			// 요청별 접근 권한 설정
			.authorizeHttpRequests(auth -> auth
					// 비로그인 허용 페이지
					.requestMatchers("/","/index","/user/userSignUpPage","/user/userLoginPage", "/user/userSignUp","/chatbot/chatBotPage","/user/analysisPage").permitAll()
					.requestMatchers("/chatbot/ask","/api/chat").permitAll()
					.requestMatchers("/user/userFindIdPage","/user/userResetPwPage","/user/userUpdatePw","/user/userCheckId","/user/userFindId").permitAll()
					.requestMatchers("/notice/noticePage","/notice/noticeDetailPage").permitAll()
					.requestMatchers("/receipt/uploadReceipt").permitAll()
					.requestMatchers("/error/**").permitAll()
					.requestMatchers("/ai/**").permitAll()
					
					// 관리자 권한	
					.requestMatchers("/admin/**","/notice/noticeRegisterPage","/noticePage/noticeRegister").hasRole("ADMIN")
					
					// 일반 사용자 권한
					.requestMatchers("/user/userInfoShowPage", "/receipt/receiptRegisterPage").hasRole("USER")
					
					// 나머지는 로그인 필수
					.anyRequest().authenticated()
					)
			
			// 로그인 설정
			.formLogin(form -> form
					.loginPage("/user/userLoginPage")
					.loginProcessingUrl("/login")
					.usernameParameter("u_id")
					.passwordParameter("u_pw")
					.defaultSuccessUrl("/", true)
					.permitAll()
				)
			
			// 로그아웃 설정
			.logout(logout -> logout
					.logoutUrl("/logout")
					.logoutSuccessUrl("/")
					.permitAll()
				);
//		// 커스텀 필터 등록 (로그인 필터 이전)
//		http.addFilterBefore(redirectLoggonFilter, UsernamePasswordAuthenticationFilter.class);
		
		// 접근 권한 거부(403) 처리
		http.exceptionHandling(handler -> handler
				.accessDeniedPage("/error/403")
		);
		return http.build();
	}
	
	// 정적 리소스는 Security 필터 제외
	@Bean
	public WebSecurityCustomizer webSecurityCustomizer() {
		return (web) -> web.ignoring().requestMatchers("/css/**","/js/**","/img/**","/favicon.ico","/health");
	}
	
	// 비밀번호 암호화 Bean
	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
}
