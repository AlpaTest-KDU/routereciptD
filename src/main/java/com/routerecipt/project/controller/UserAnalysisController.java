package com.routerecipt.project.controller;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.routerecipt.project.dto.ReceiptAnalysisStatsdto;
import com.routerecipt.project.dto.Userdto;
import com.routerecipt.project.receipt.ReceiptResultService;
import com.routerecipt.project.security.LoginDetails;

import lombok.RequiredArgsConstructor;

@ControllerAdvice
@RequiredArgsConstructor
public class UserAnalysisController {

    private final ReceiptResultService receiptResultService;

    @ModelAttribute
    public void injectAnalysisStats(Model model, HttpServletRequest request) {

        // ✅ /user/analysisPage 요청에서만 동작 (다른 페이지 영향 방지)
        if (!"/user/analysisPage".equals(request.getRequestURI())) {
            return;
        }

        // ✅ 로그인 사용자 확인
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            // 여기서 redirect는 불가(ControllerAdvice의 @ModelAttribute는 흐름 제어용이 아님)
            // /user/analysisPage 접근 차단은 SecurityConfig에서 처리하는 게 정석
            return;
        }

        LoginDetails loginDetails = (LoginDetails) auth.getPrincipal();
        Userdto loginUser = loginDetails.getUser();
        String uId = loginUser.getU_id();

        // ✅ 통계 조회 + 모델 바인딩
        ReceiptAnalysisStatsdto stats = receiptResultService.getStats(uId);

        model.addAttribute("dailyData",  stats.getDailyData());
        model.addAttribute("weeklyData", stats.getWeeklyData());
        model.addAttribute("monthlyData", stats.getMonthlyData());
        model.addAttribute("genderData", stats.getGenderData());
        model.addAttribute("myAvg",      stats.getMyAvg());
        model.addAttribute("allAvg",     stats.getAllAvg());
    }
}
