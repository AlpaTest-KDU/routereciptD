package com.routerecipt.project.dto;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Userdto {
	private String u_id;
	private String u_pw;
	private Date u_birthday;
	private String u_email;
	private Date u_date;
	private Gender gender;
	private Role role;
	
	public enum Gender {
		MALE,FEMALE
	}
}
