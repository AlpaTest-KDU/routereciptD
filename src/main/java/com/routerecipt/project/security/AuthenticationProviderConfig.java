package com.routerecipt.project.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AuthenticationProviderConfig {
	
	@Bean
	public AuthenticationProvider authenticationProvider(
			LoginDetailsService loginDetailsService,
			PasswordEncoder passwordEncoder
			) {
		
		DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
		provider.setUser
	}
}
