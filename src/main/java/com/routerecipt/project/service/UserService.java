package com.routerecipt.project.service;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.routerecipt.project.dto.Userdto;

/**
 * 회원(User) 도메인 서비스 인터페이스
 *
 * - 회원가입, 로그인용 조회, 계정 찾기, 수정/삭제 등
 *   사용자 관련 비즈니스 기능을 정의한다.
 * - 컨트롤러는 이 인터페이스를 통해 사용자 기능을 호출한다.
 */
public interface UserService {
	void UserSignUp(Userdto u);					// 회원가입
	Userdto loadUserByUsername(String u_id);	// 로그인/인증용 사용자 조회
	Userdto UserFindID (String u_email);		// 이메일로 사용자 ID 찾기
	int UserCheckID(@Param("u_id") String u_id, @Param("u_email") String email);	// 사용자 ID 중복 여부 확인
	void UserUpdatePW (Userdto u);				// 비밀번호 변경/재설정
	List<Userdto> UserInfoShow();				// 전체 사용자 목록 조회 (관리자용)
	void UserInfoUpdate(Userdto u);				// 사용자 정보 수정
	void UserInfoDelete(String u_id);			// 사용자 계정 삭제
	Userdto UserSelectById(String u_id);		// 사용자 단건 조회
}
