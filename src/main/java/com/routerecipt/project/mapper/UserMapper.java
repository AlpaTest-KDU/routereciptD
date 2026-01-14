package com.routerecipt.project.mapper;


import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.routerecipt.project.dto.UserDTO;


/**
 * 사용자(User) MyBatis Mapper
 *
 * 역할:
 *  - 회원 가입 / 로그인 / 계정 관리 관련 DB 접근 담당
 *  - Spring Security 인증 흐름과 직접 연동
 */
@Mapper
public interface UserMapper {
	void UserSignUp(UserDTO u);					// 회원 가입
	UserDTO loadUserByUsername(@Param("username") String username);	// 로그인 및 인증을 위한 사용자 조회
	UserDTO UserSelectById(@Param("u_id") String u_id);		// 사용자 ID로 단건 조회
	UserDTO UserFindID (String u_email);		// 이메일로 사용자 ID 찾기
	int UserCheckID(@Param("u_id") String u_id, @Param("u_email") String email);	// 사용자 ID + 이메일 일치 여부 확인
	void UserUpdatePW (UserDTO u);				// 사용자 비밀번호 변경
	void UserInfoUpdate(UserDTO u);				// 사용자 정보 수정
	void UserInfoDelete(String u_id);			// 사용자 계정 삭제
}	
