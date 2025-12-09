package com.routerecipt.project.service;

import org.springframework.beans.factory.annotation.Autowired;

import com.routerecipt.project.mapper.UserMapper;

public class UserServiceImp implements UserService {
	
	@Autowired
	private UserMapper userMapper;
	
}
