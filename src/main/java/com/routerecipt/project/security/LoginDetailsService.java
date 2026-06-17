package com.routerecipt.project.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.routerecipt.project.entity.User;
import com.routerecipt.project.repository.UserRepository;


/**
 * Spring Security에서 로그인 시 사용자 정보를 조회하는 서비스
 * username(ID)을 기준으로 DB에서 유저를 조회한다.
 * 기존엔 UserMapper를 직접 주입받았지만, JPA 전환 후 UserRepository로 교체
 */
@Service
public class LoginDetailsService implements UserDetailsService {
	
	// 사용자 DB 조회를 담당하는 Mapper
//	private final UserMapper userMapper;
	private final UserRepository userRepository;
	
	// 생성자 주입
//	public LoginDetailsService(UserMapper userMapper) {
//		this.userMapper = userMapper;
//	}
	
	public LoginDetailsService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}
	
	// 로그인 요청 시 Spring Security가 자동으로 호출하는 메서드
//	@Override
//	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
//		
//		// 아이디(username)로 사용자 조회
//		UserDTO user = userMapper.loadUserByUsername(username);
//		
//		 // 사용자가 존재하지 않으면 인증 실패 처리
//		if (user == null) {
//			throw new UsernameNotFoundException("유저를 찾을 수 없습니다" + username);
//		}
//		
//		 // Userdto를 UserDetails 구현체로 감싸서 반환
//		return new LoginDetails(user);
//	}
	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		// findById -> PK(u_id)로 단건 조회, 없으면 예외 발생
		User user = userRepository.findById(username)
				.orElseThrow(() -> new UsernameNotFoundException("유저를 찾을 수 없습니다: " + username));
		return new LoginDetails(user);
	}
	
	
}
