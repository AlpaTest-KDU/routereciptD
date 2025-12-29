package com.routerecipt.project.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.routerecipt.project.dto.Userdto;
import com.routerecipt.project.dto.ReceiptAnalysisStatsdto;
import com.routerecipt.project.receipt.ReceiptResultService;
import com.routerecipt.project.security.LoginDetails; // ✅ Security 인증 객체

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserAnalysisController {

    private final ReceiptResultService receiptResultService;

    @GetMapping("/analysisPage")
    public String analysisPage(Model model) {

        // ✅ 1. 현재 로그인한 사용자 정보 가져오기 (Spring Security 기반)
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return "redirect:/"; // 로그인 안 된 사용자 처리
        }

        // ✅ 2. principal → LoginDetails → Userdto 추출
        LoginDetails loginDetails = (LoginDetails) auth.getPrincipal();
        Userdto loginUser = loginDetails.getUser();

        // ✅ 3. 로그인 사용자 ID
        String uId = loginUser.getU_id();

        // ✅ 4. 서비스에서 통계 조회
        ReceiptAnalysisStatsdto stats = receiptResultService.getStats(uId);

        // ✅ 5. 모델 바인딩
        model.addAttribute("dailyData",  stats.getDailyData());
        model.addAttribute("weeklyData", stats.getWeeklyData());
        model.addAttribute("monthlyData", stats.getMonthlyData());
        model.addAttribute("genderData", stats.getGenderData());
        model.addAttribute("myAvg",      stats.getMyAvg());
        model.addAttribute("allAvg",     stats.getAllAvg());

        // ✅ 6. 뷰 반환
        return "user/analysisPage";
    }
}
