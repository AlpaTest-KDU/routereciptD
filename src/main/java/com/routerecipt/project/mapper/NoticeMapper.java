package com.routerecipt.project.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.routerecipt.project.dto.Criteria;
import com.routerecipt.project.dto.Noticedto;

@Mapper
public interface NoticeMapper {
	void NoticeRegister (Noticedto n);
	List<Noticedto>NoticeShow();
	Noticedto findById(int n_id);
	// 추후 기능 변경(EX: 검색)이 있을 경우 Criteria에 추가해서 사용할 예정
	// 그에 따라 파라미터 유지
	int getTotalNoticeCount(Criteria criteria);
	List<Noticedto> NoticeShowWithPage(Criteria criteria);
}
