package com.routerecipt.project.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.routerecipt.project.dto.Criteria;
import com.routerecipt.project.dto.Noticedto;


/**
 * 공지사항(Notice) MyBatis Mapper
 *
 * 역할:
 *  - 공지사항 테이블에 대한 DB 접근(SQL 실행)을 담당
 *  - 등록, 단건 조회, 목록 조회, 페이징 조회 기능 제공
 */
@Mapper
public interface NoticeMapper {
	
	/**
     * 공지사항 등록
     *
     * @param n 등록할 공지사항 DTO
     */
	void NoticeRegister (Noticedto n);
	
	/**
     * 공지사항 전체 목록 조회 (비페이징)
     *
     * @return 공지사항 목록
     */
	List<Noticedto>NoticeShow();
	
	/**
     * 공지사항 단건 조회
     *
     * @param n_id 공지사항 ID
     * @return 공지사항 DTO
     */
	Noticedto findById(int n_id);
	
	/**
     * 공지사항 전체 개수 조회 (페이징용)
     *
     * @param criteria 페이지/검색 조건
     * @return 전체 공지 개수
     */
	int getTotalNoticeCount(Criteria criteria);
	
	/**
     * 공지사항 페이징 조회
     *
     * @param criteria 페이지/검색 조건
     * @return 공지사항 목록
     */
	List<Noticedto> NoticeShowWithPage(Criteria criteria);
}
