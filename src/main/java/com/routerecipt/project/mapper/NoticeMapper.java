package com.routerecipt.project.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.routerecipt.project.dto.Noticedto;

@Mapper
public interface NoticeMapper {
	void NoticeRegister (Noticedto n);
	List<Noticedto>NoticeShow();
	Noticedto findById(int n_id);
}
