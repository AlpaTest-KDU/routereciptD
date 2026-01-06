package com.routerecipt.project.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;


/**
 * 사용자 권한(Role) Enum
 *
 * 역할:
 *  - 애플리케이션에서 사용하는 사용자 권한을 정의
 *  - Spring Security 인증/인가 처리에 사용
 */
@Getter
@AllArgsConstructor
public enum Role {
	ROLE_USER("ROLE_USER"),		// 일반 사용자 권한
	ROLE_ADMIN("ROLE_ADMIN");	// 관리자 권한
	
	private final String role;	// 실제 권한 문자열 값
}
