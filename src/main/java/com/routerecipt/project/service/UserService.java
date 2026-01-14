package com.routerecipt.project.service;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.routerecipt.project.dto.UserDTO;

/**
 * 회원(User) 도메인 서비스 인터페이스
 *
 * - 회원가입, 로그인용 조회, 계정 찾기, 수정/삭제 등
 *   사용자 관련 비즈니스 기능을 정의한다.
 * - 컨트롤러는 이 인터페이스를 통해 사용자 기능을 호출한다.
 */
public interface UserService {
	void UserSignUp(UserDTO u);					// 회원가입
	UserDTO loadUserByUsername(@Param("username") String username);	// 로그인/인증용 사용자 조회
	UserDTO UserFindID (String u_email);		// 이메일로 사용자 ID 찾기
	int UserCheckID(@Param("u_id") String u_id, @Param("u_email") String email);	// 사용자 ID 중복 여부 확인
	void UserUpdatePW (UserDTO u);				// 비밀번호 변경/재설정
	void UserInfoUpdate(UserDTO u);				// 사용자 정보 수정
	void UserInfoDelete(String u_id);			// 사용자 계정 삭제
	UserDTO UserSelectById(String u_id);		// 사용자 단건 조회
}
