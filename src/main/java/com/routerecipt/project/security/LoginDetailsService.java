package com.routerecipt.project.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.routerecipt.project.dto.Userdto;
import com.routerecipt.project.mapper.UserMapper;


/**
 * Spring Security에서 로그인 시 사용자 정보를 조회하는 서비스
 * username(ID)을 기준으로 DB에서 유저를 조회한다.
 */
@Service
public class LoginDetailsService implements UserDetailsService {
	
	// 사용자 DB 조회를 담당하는 Mapper
	private final UserMapper userMapper;
	
	// 생성자 주입
	public LoginDetailsService(UserMapper userMapper) {
		this.userMapper = userMapper;
	}
	
	// 로그인 요청 시 Spring Security가 자동으로 호출하는 메서드
	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		
		// 아이디(username)로 사용자 조회
		Userdto user = userMapper.loadUserByUsername(username);
		
		 // 사용자가 존재하지 않으면 인증 실패 처리
		if (user == null) {
			throw new UsernameNotFoundException("유저를 찾을 수 없습니다" + username);
		}
		
		 // Userdto를 UserDetails 구현체로 감싸서 반환
		return new LoginDetails(user);
	}
	
	
}
