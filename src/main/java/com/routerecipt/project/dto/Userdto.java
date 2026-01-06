package com.routerecipt.project.dto;

import java.util.Date;

import org.springframework.format.annotation.DateTimeFormat;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


/**
 * 사용자(User) DTO
 *
 * 역할:
 *  - 회원 1명의 기본 정보를 담는 데이터 전송 객체
 *  - 로그인, 권한, 마이페이지, 통계 기능의 기준 데이터
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Userdto {
	private String u_id;					// 사용자 ID (로그인 ID, Primary Key)
	private String u_pw;					// 사용자 비밀번호
	private String u_name;					// 사용자 이름
	@DateTimeFormat(pattern = "yyyy-MM-dd")	// 사용자 생년월일(yyyy-MM-dd 형식으로 바인딩)
	private Date u_birthday;
	private String u_email;					// 사용자 이메일
	private Date u_date;					// 회원 가입일
	private Gender gender;					// 사용자 성별
	private Role role;						// 사용자 권한(ROLE_USER / ROLE_ADMIN)
		
	public enum Gender {
		MALE,FEMALE
	}
}
