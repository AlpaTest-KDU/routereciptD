package com.routerecipt.project.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.routerecipt.project.dto.Userdto;
import com.routerecipt.project.mapper.UserMapper;
import com.routerecipt.project.redis.BloomFilter.RedisBloomService;

@Service
public class UserServiceImp implements UserService {
	
	@Autowired
	private UserMapper userMapper;
	
	@Autowired
	private RedisBloomService bloomService;
	
	@Autowired
	private PasswordEncoder passwordEncoder;
	
	public boolean checkDuplicateUserId(String userId) {

	    // 1단계: Bloom Filter 판단
	    if (bloomService.isDuplicate(userId)) {
	        // Bloom Filter는 false positive 가능 → DB로 확정 검사
	        Userdto user = userMapper.UserFindID(userId);
	        return user != null; // true면 중복
	    }

	    // 2단계: Bloom Filter에 없음 → DB 확인
	    Userdto user = userMapper.UserFindID(userId);

	    if (user == null) {
	        // DB에도 없으면 Bloom Filter에 등록
	        bloomService.register(userId);
	        return false; // 중복 아님
	    }

	    return true; // DB에 존재 → 중복
	}

	
	// Security 로그인 전용
	@Override
	public Userdto loadUserByUsername(String u_id) {
		return userMapper.loadUserByUsername(u_id);
	}
	
	@Override
	@Transactional
	public void UserSignUp(Userdto u) {
		userMapper.UserSignUp(u);
	}
	
	// 계정 찾기
	@Override
	public Userdto UserFindID(String u_email) {
		return userMapper.UserFindID(u_email);
	}

	// 비밀번호 변경 전 계정 확인
	@Override
	public int UserCheckID(String u_id, String email) {
		return userMapper.UserCheckID(u_id, email);
	}
	
	// 비밀번호 변경
	@Override
	public void UserUpdatePW(Userdto u) {
		String encodedPw = passwordEncoder.encode(u.getU_pw());
		u.setU_pw(encodedPw);
		userMapper.UserUpdatePW(u);
	}
	
	// 회원 탈퇴
	@Override
	public void UserInfoDelete(String u_id) {
		userMapper.UserInfoDelete(u_id);
	}
	
	// 회원 정보 보기
	@Override
	public List<Userdto> UserInfoShow() {
		return userMapper.UserInfoShow();
	}
	
	// 회원 정보 name으로 찾기
	@Override
	public List<Userdto> selectUserByName(String u_name) {
		return userMapper.selectUserByName(u_name);
	}

	// 회원 정보 id로 찾기
	@Override
	public Userdto selectUserById(String u_id) {
		return userMapper.selectUserById(u_id);
	}
	
	// 회원 정보 수정
	@Override
	public void UserInfoUpdate(Userdto u) {
		userMapper.UserInfoUpdate(u);
	}
}
