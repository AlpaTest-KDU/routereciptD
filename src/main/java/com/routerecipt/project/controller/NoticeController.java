package com.routerecipt.project.controller;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.routerecipt.project.dto.Criteria;
import com.routerecipt.project.dto.Noticedto;
import com.routerecipt.project.dto.Pagedto;
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

	// 공지사항 페이지
	@GetMapping("/noticePage")
	public String noticePage(Criteria criteria, Model model) {
		model.addAttribute("noticeList", noticeServiceImp.NoticeShowWithPage(criteria));

		int totalPage = noticeServiceImp.getTotalNoticeCount(criteria);
		model.addAttribute("page", new Pagedto(criteria, totalPage));
		return "notice/noticePage";
	}
	
}	
