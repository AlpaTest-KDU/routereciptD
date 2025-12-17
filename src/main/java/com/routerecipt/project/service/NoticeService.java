package com.routerecipt.project.service;

import java.util.List;

import com.routerecipt.project.dto.Noticedto;

public interface NoticeService {
	void NoticeRegister (Noticedto n);
	List<Noticedto>NoticeShow();
	Noticedto findById(int n_id);
}
