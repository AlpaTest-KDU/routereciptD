package com.routerecipt.project.service;

import java.util.List;
import java.util.Map;

import com.routerecipt.project.dto.Userdto;

public interface UserService {
	void UserSignUp(Userdto u);
	Userdto loadUserByUsername(String u_id);
	Userdto UserFind(String u_eamil);
	List<Userdto> UserInfoShow();
	void UserInfoUpdate(Userdto u);
	void UserInfoDelete(Userdto u);
	
}
