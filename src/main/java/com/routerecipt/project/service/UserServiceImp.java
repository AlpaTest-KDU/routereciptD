package com.routerecipt.project.service;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;

import com.routerecipt.project.dto.Userdto;
import com.routerecipt.project.mapper.UserMapper;

public class UserServiceImp implements UserService {
	
	@Autowired
	private UserMapper userMapper;
	
	// Security 로그인 전용
	@Override
	public Userdto loadUserByUsername(String u_id) {
		return userMapper.loadUserByUsername(u_id);
	}
	
	@Override
	public void UserSignUp(Userdto u) {
		userMapper.UserSignUp(u);
	}
	
	// 계정 찾기
	@Override
	public Userdto UserFind(Map<String, Object> map) {
		return userMapper.UserFind(map);
	}
	
	// 회원 탈퇴
	@Override
	public void UserInfoDelete(Userdto u) {
		userMapper.UserInfoDelete(u);
	}
	
	// 회원 정보 보기
	@Override
	public List<Userdto> UserInfoShow() {
		return userMapper.UserInfoShow();
	}
	
	// 회원 정보 수정
	@Override
	public void UserInfoUpdate(Userdto u) {
		userMapper.UserInfoUpdate(u);
	}
	
	
}
