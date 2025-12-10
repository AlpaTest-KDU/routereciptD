package com.routerecipt.project.mapper;


import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;

import com.routerecipt.project.dto.Userdto;

@Mapper
public interface UserMapper {
	void UserSignUp(Userdto u);
	Userdto loadUserByUsername(String u_id);
	Userdto UserFind(String u_email);
	List<Userdto> UserInfoShow();
	void UserInfoUpdate(Userdto u);
	void UserInfoDelete(Userdto u);
}
