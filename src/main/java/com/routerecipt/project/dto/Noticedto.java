package com.routerecipt.project.dto;

import java.time.LocalDateTime;
import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


/**
 * 공지사항(Notice) DTO
 *
 * 역할:
 *  - 공지사항 게시글 1건의 정보를 담는 데이터 전송 객체
 *  - 게시판/공지 기능에서 Controller ↔ Service ↔ DB 간 데이터 전달
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Noticedto {
	private int n_id;			// 공지사항 고유 ID (Primary Key)
	private String n_title;		// 공지사항 제목
	private String n_writer;	// 공지사항 작성자(보통 관리자)
	private String n_content;	// 공지사항 본문 내용
	private Date n_create;		// 공지사항 작성일
}
