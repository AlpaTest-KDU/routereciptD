package com.routerecipt.project.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.routerecipt.project.dto.Userdto;
import com.routerecipt.project.mapper.UserMapper;

@Service
public class LoginDetailsService implements UserDetailsService {
	
	private final UserMapper userMapper;
	
	public LoginDetailsService(UserMapper userMapper) {
		this.userMapper = userMapper;
	}
	
	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		Userdto user = userMapper.UserFind(username);
		
		if (user == null) {
			throw new UsernameNotFoundException("유저를 찾을 수 없습니다" + username);
		}
		
		return new LoginDetails(user);
	}
	
	
}
