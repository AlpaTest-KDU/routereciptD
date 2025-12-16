package com.routerecipt.project.mapper;


import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;

import com.routerecipt.project.dto.Userdto;

@Mapper
public interface UserMapper {
	void UserSignUp(Userdto u);
	Userdto loadUserByUsername(String u_id);
	Userdto UserFindID (String u_email);
	Userdto UserCheckID (String u_id, String u_email);
	void UserUpdatePW (Userdto u);
	List<Userdto> UserInfoShow();
	void UserInfoUpdate(Userdto u);
	void UserInfoDelete(String u_id);
}
