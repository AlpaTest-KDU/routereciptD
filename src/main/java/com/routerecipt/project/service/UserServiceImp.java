package com.routerecipt.project.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.routerecipt.project.dto.UserDTO;
import com.routerecipt.project.mapper.UserMapper;
import com.routerecipt.project.redis.BloomFilter.RedisBloomService;

/**
 * 회원(User) 관련 서비스 구현체
 *
 * - 회원가입/조회/수정/탈퇴/비밀번호 변경 등 사용자 비즈니스 로직 수행
 * - Bloom Filter(Redis)로 아이디 중복 체크를 최적화한다.
 */
@Service
public class UserServiceImp implements UserService {
	
	@Autowired
	private UserMapper userMapper;
	
	@Autowired
	private RedisBloomService bloomService;
	
	@Autowired
	private PasswordEncoder passwordEncoder;
	
	// 사용자 아이디 중복 여부 확인
	/**
    * 동작:
    * 1) Bloom Filter에 "있다"면(중복 가능) → DB로 확정 검사(오탐 가능성 때문)
    * 2) Bloom Filter에 "없다"면 → DB 확인 후 없으면 Bloom Filter에 등록
    */
	public boolean checkDuplicateUserId(String userId) {

	    // 1단계: Bloom Filter 판단
	    if (bloomService.isDuplicate(userId)) {
	        // Bloom Filter는 false positive 가능 → DB로 확정 검사
	        UserDTO user = userMapper.UserFindID(userId);
	        return user != null; // true면 중복
	    }

	    // 2단계: Bloom Filter에 없음 → DB 확인
	    UserDTO user = userMapper.UserFindID(userId);

	    if (user == null) {
	        // DB에도 없으면 Bloom Filter에 등록
	        bloomService.register(userId);
	        return false; // 중복 아님
	    }

	    return true; // DB에 존재 → 중복
	}

	
	// Spring Security 로그인 전용 사용자 조회
	@Override
	public UserDTO loadUserByUsername(String u_id) {
		return userMapper.loadUserByUsername(u_id);
	}
	
	// 회원가입 
	@Override
	@Transactional
	public void UserSignUp(UserDTO u) {
		userMapper.UserSignUp(u);
	}
	
	// 이메일로 계정 찾기
	@Override
	public UserDTO UserFindID(String u_email) {
		return userMapper.UserFindID(u_email);
	}

	// 비밀번호 변경 전 계정 확인
	@Override
	public int UserCheckID(String u_id, String email) {
		return userMapper.UserCheckID(u_id, email);
	}
	
	// 비밀번호 변경
	@Override
	public void UserUpdatePW(UserDTO u) {
		String encodedPw = passwordEncoder.encode(u.getU_pw());
		u.setU_pw(encodedPw);
		userMapper.UserUpdatePW(u);		// DB 반영
	}
	
	// 회원 탈퇴
	@Override
	public void UserInfoDelete(String u_id) {
		userMapper.UserInfoDelete(u_id);
	}
	
	
	// 회원 정보 id로 찾기
	@Override
	public UserDTO UserSelectById(String u_id) {
		return userMapper.UserSelectById(u_id);
	}
	
	// 회원 정보 수정
	@Override
	public void UserInfoUpdate(UserDTO u) {
		userMapper.UserInfoUpdate(u);
	}
}
