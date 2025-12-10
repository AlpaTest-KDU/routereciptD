package com.routerecipt.project.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class UserfindController {
	
	@GetMapping("/userPasswordChangePage")
	public String userPasswordChangePage(
			@RequestParam(value = "id") String id,
			@RequestParam(value = "email") String email) {
		return "userPasswordChagePage";
	}
}
