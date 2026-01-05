package com.routerecipt.project.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;


@Configuration
@EnableWebSecurity(debug = true)
public class SecurityConfig {

	@Bean
	public SecurityFilterChain filterChan(HttpSecurity http, RedirectLoggonFilter redirectLoggonFilter) throws Exception {
		http
			.authorizeHttpRequests(auth -> auth
					.requestMatchers("/","/index","/user/userSignUpPage","/user/userLoginPage", "/user/userSignUp","/chatbot/chatBotPage","user/analysisPage").permitAll()
					.requestMatchers("/chatbot/ask","/api/chat").permitAll()
					.requestMatchers("/user/userFindIdPage","/user/userResetPwPage","/user/userUpdatePw","/user/userCheckId","/user/userFindId").permitAll()
					.requestMatchers("/notice/noticePage","/notice/noticeDetailPage").permitAll()
					.requestMatchers("/error/**").permitAll()
					.requestMatchers("/ai/**").permitAll()
					.requestMatchers("/admin/**","/notice/noticeRegisterPage","/noticePage/noticeRegister").hasRole("ADMIN")
					.requestMatchers("/user/userInfoShowPage", "/recepit/receiptRegisterPage").hasRole("USER")
					.anyRequest().authenticated()
					)
			.formLogin(form -> form
					.loginPage("/user/userLoginPage")
					.loginProcessingUrl("/login")
					.usernameParameter("u_id")
					.passwordParameter("u_pw")
					.defaultSuccessUrl("/")
					.permitAll()
				)
			.logout(logout -> logout
					.logoutUrl("/logout")
					.logoutSuccessUrl("/")
					.permitAll()
				);
		
		http.addFilterBefore(redirectLoggonFilter, UsernamePasswordAuthenticationFilter.class);
		http.exceptionHandling(handler -> handler
				.accessDeniedPage("/error/403")
		);
		return http.build();
	}
	
	@Bean
	public WebSecurityCustomizer webSecurityCustomizer() {
		return (web) -> web.ignoring().requestMatchers("/css/**","/js/**","/img/**","/favicon.ico");
	}
	
	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
}
