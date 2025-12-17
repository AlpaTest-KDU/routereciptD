package com.routerecipt.project.dto;

import java.time.LocalDateTime;
import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Noticedto {
	private int n_id;
	private String n_title;
	private String n_writer;
	private String n_content;
	private Date n_create;
}
