package com.routerecipt.project.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.AntPathMatcher;


@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Bean
	public SecurityFilterChain filterChan(HttpSecurity http, RedirectLoggonFilter redirectLoggonFilter, AuthenticationProvider authenticationProvider) throws Exception {
		http
			.authorizeHttpRequests(auth -> auth
					.requestMatchers("/","index","/user/UserSignUpPage","/user/UserLoginPage").permitAll()
					.requestMatchers("/admin/**").hasRole("ADMIN")
					.anyRequest().authenticated()
					)
			.formLogin(form -> form
					.loginPage("login")
					.loginProcessingUrl("/login")
					.usernameParameter("u_id")
					.passwordParameter("u_pw")
					.defaultSuccessUrl("/")
					.permitAll()
				)
			.logout(logout -> logout
					.logoutRequestMatcher((RequestMatcher) new AntPathMatcher("/logout"))
					.logoutSuccessUrl("/")
					.permitAll()
				);
		
		http.authenticationProvider(authenticationProvider);
		http.addFilterBefore(redirectLoggonFilter, UsernamePasswordAuthenticationFilter.class);
		
		return http.build();
	}
	
	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
}
