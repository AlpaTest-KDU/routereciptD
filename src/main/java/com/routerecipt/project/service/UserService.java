package com.routerecipt.project.service;

import java.util.List;
import java.util.Map;

import com.routerecipt.project.dto.Userdto;

public interface UserService {
	void UserSignUp(Userdto u);
	Userdto loadUserByUsername(String u_id);
	Userdto UserFindID (String u_email);
	Userdto UserCheckID (String u_id, String u_email);
	void UserUpdatePW (Userdto u);
	List<Userdto> UserInfoShow();
	void UserInfoUpdate(Userdto u);
	void UserInfoDelete(String u_id);
	
}
