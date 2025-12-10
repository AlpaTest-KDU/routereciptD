package com.routerecipt.project.security;

import java.util.ArrayList;
import java.util.Collection;

import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.routerecipt.project.dto.Userdto;

public class LoginDetails implements UserDetails{
	
	private static final long serialVersionUID = 1L;
	
	private Userdto userdto;
	
	public LoginDetails(Userdto userdto) {
		this.userdto = userdto;
	}
	
	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		Collection<GrantedAuthority> collections = new ArrayList<>();
		collections.add(()->{
			return this.userdto.getRole().name();
		});
		return collections;
	}
	
	@Override
	public @Nullable String getPassword() {
		return this.userdto.getU_pw();
	}
	
	@Override
	public String getUsername() {
		return this.userdto.getU_id();
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
