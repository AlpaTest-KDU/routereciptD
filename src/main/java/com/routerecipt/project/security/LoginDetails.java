package com.routerecipt.project.security;

import java.util.ArrayList;
import java.util.Collection;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.routerecipt.project.dto.Userdto;


/**
 * Spring Security에서 사용하는 사용자 인증 정보 클래스
 * Userdto를 Security가 인식할 수 있도록 감싸는 역할
 */
public class LoginDetails implements UserDetails{
	
	
	private static final long serialVersionUID = 1L;
	
	// 실제 사용자 정보 (DB에서 조회한 DTO)
	private Userdto userdto;
	
	// 생성자 - 로그인 성공 시 UserDetailsService에서 주입
	public LoginDetails(Userdto userdto) {
		this.userdto = userdto;
	}
	
	// 로그인한 사용자 정보 반환 (컨트롤러/서비스에서 사용)
	public Userdto getUser() {
		return userdto;
	}
	
	
	public void setUser(Userdto userdto) {
		this.userdto = userdto;
	}
	
	// 사용자 권한 목록 반환
	@Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Collection<GrantedAuthority> authorities = new ArrayList<>();
        
        // 사용자 권한이 존재하면 추가
        if (userdto != null && userdto.getRole() != null) {
            authorities.add(() -> userdto.getRole().name());
        }
        return authorities;
    }
	
	// 비밀번호 반환 (암호화된 상태)
	@Override
	public String getPassword() {
		return this.userdto.getU_pw();
	}
	
	// 로그인 아이디 반환
	@Override
	public String getUsername() {
		return userdto != null ? userdto.getU_id() : "";
	}
	
	// 계정 만료 여부
	@Override
	public boolean isAccountNonExpired() {
		return true;
	}
	
	// 계정 잠김 여부
	@Override
	public boolean isAccountNonLocked() {
		return true;
	}
	
	// 비밀번호 만료 여부
	@Override
	public boolean isCredentialsNonExpired() {
		return true;
	}
	
	// 계정 활성화 여부
	@Override
	public boolean isEnabled() {
		return true;
	}
	
}
