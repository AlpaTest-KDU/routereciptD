package com.routerecipt.project.dto;

import java.util.Date;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
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
	@NotBlank(message = "아이디를 입력해주세요")
	@Size(min=6, max=16, message = "아이디는 6~16자여야 합니다.")
	@Pattern(regexp = "^[a-zA-Z0-9]*$", message = "아이디는 영문/숫자 조합만 가능합니다.")
	private String u_id;

	@NotBlank(message = "비밀번호를 입력해주세요")
	@Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*[0-9])(?=.*[!@#$%^&*]).{6,24}$", message = "비밀번호는 영문, 숫자, 특수문자를 포함한 6~24자여야 합니다.")
	private String u_pw;

	@NotBlank(message = "이름을 입력해주세요")
	private String u_name;

	@NotNull
	@DateTimeFormat(pattern = "yyyy-MM-dd")
	private Date u_birthday;

	@NotBlank
	private String u_email;

	private Date u_date;

	@NotNull
	private Gender gender;

	private Role role;
	
	public enum Gender {
		MALE,FEMALE
	}
}
