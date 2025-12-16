package com.routerecipt.project.security;

import java.util.ArrayList;
import java.util.Collection;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.routerecipt.project.dto.Userdto;

public class LoginDetails implements UserDetails{
	
	private static final long serialVersionUID = 1L;
	
	private Userdto userdto;
	
	public LoginDetails(Userdto userdto) {
		this.userdto = userdto;
	}

	public Userdto getUser() {
		return userdto;
	}
	
	public void setUser(Userdto userdto) {
		this.userdto = userdto;
	}
	
	@Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Collection<GrantedAuthority> authorities = new ArrayList<>();
        if (userdto != null && userdto.getRole() != null) {
            authorities.add(() -> userdto.getRole().name());
        }
        return authorities;
    }
	
	@Override
	public String getPassword() {
		return this.userdto.getU_pw();
	}
	
	@Override
	public String getUsername() {
		return userdto != null ? userdto.getU_id() : "";
	}
	
	@Override
	public boolean isAccountNonExpired() {
		return true;
	}
	
	@Override
	public boolean isAccountNonLocked() {
		return true;
	}
	
	@Override
	public boolean isCredentialsNonExpired() {
		return true;
	}
	
	@Override
	public boolean isEnabled() {
		return true;
	}
	
}
