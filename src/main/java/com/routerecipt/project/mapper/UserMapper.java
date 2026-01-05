package com.routerecipt.project.mapper;


import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.routerecipt.project.dto.Userdto;

@Mapper
public interface UserMapper {
	void UserSignUp(Userdto u);
	Userdto loadUserByUsername(String u_id);
	Userdto UserSelectById(String u_id);
	Userdto UserFindID (String u_email);
	int UserCheckID(@Param("u_id") String u_id, @Param("u_email") String email);
	void UserUpdatePW (Userdto u);
	List<Userdto> UserInfoShow();
	void UserInfoUpdate(Userdto u);
	void UserInfoDelete(String u_id);
}
