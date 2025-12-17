package com.routerecipt.project.controller;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.routerecipt.project.dto.Noticedto;
import com.routerecipt.project.security.LoginDetails;
import com.routerecipt.project.service.NoticeServiceImp;

@Controller
@RequestMapping("/notice")
public class NoticeController {
	
	@Autowired
	private NoticeServiceImp noticeServiceImp;
	
	// 공지사항 등록 화면
	@GetMapping("/noticeRegisterPage")
	public String noticeRegisterPage() {
		return "notice/noticeRegisterPage";
	}

	// 공지사항 등록 기능
	@PostMapping("/noticeRegist")
	public String noticeRegist(Noticedto n, Authentication authentication) {
		LoginDetails principal = (LoginDetails) authentication.getPrincipal();
		
		n.setN_writer(principal.getUser().getU_id());
		
		noticeServiceImp.NoticeRegister(n);
		return "redirect:/notice/noticePage";
	}
	
	// 공지사항 내용 화면
	@GetMapping("/noticeDetailPage")
	public String noticeDetailPage(@RequestParam("n_id") int n_id, Model model) {
		Noticedto notice = noticeServiceImp.findById(n_id);
		
		if (notice == null) {
			throw new IllegalArgumentException("존재하지 않는 공지입니다. n_id=" + n_id);
		}
		
		model.addAttribute("notice",notice);
		return "notice/noticeDetailPage";
	}
	
	// 공지사항 검색 기능
	// 1. 검색란이 비어 있으면 모든 공지사항을 보여줌
	// 2. 검색을 하면 해당하는 단어가 포함된 제목을 가진 공지사항을 보여줌
	@PostMapping("/noticeSearch")
	public String noticeSearch(Model model, @RequestParam(value="title", required=false) String title) {
		return "notice/noticePage";
	}
}	
