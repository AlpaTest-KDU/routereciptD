package com.routerecipt.project.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.routerecipt.project.dto.Userdto;

@Mapper
public interface UserMapper {
	
	Userdto UserFind(String u_name);
}
