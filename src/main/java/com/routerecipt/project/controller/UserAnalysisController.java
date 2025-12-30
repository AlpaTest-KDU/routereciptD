package com.routerecipt.project.controller;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.routerecipt.project.dto.ReceiptAnalysisStatsdto;
import com.routerecipt.project.receipt.ReceiptResultService;
import com.routerecipt.project.security.LoginDetails;

import lombok.RequiredArgsConstructor;

@ControllerAdvice
@RequiredArgsConstructor
public class UserAnalysisController {

    private final ReceiptResultService receiptResultService;

    @ModelAttribute
    public void injectAnalysisStats(Model model, HttpServletRequest request) {

        String uri = request.getRequestURI();
        if (uri == null) return;

        // ✅ 두 페이지만 타겟 (다른 페이지 영향 방지)
        boolean isMonthlyPage = "/user/analysisMonthlyPage".equals(uri);
        boolean isAnalysisPage = "/user/analysisPage".equals(uri);
        if (!isMonthlyPage && !isAnalysisPage) return;

        // ✅ 로그인 사용자 확인
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return;
        }

        // ✅ 캐스팅 안전 처리 (예: 다른 Authentication 타입일 때 방어)
        Object principal = auth.getPrincipal();
        if (!(principal instanceof LoginDetails loginDetails)) {
            return;
        }

        String uId = loginDetails.getUser().getU_id();

        // ✅ /user/analysisMonthlyPage : 1,2,3만
        if (isMonthlyPage) {
            ReceiptAnalysisStatsdto stats = receiptResultService.getAnalysisMonthlyPageStats(uId);
            model.addAttribute("dailyData", stats.getDailyData());
            model.addAttribute("weeklyData", stats.getWeeklyData());
            model.addAttribute("monthlyData", stats.getMonthlyData());
            return;
        }

        // ✅ /user/analysisPage : 4,5,6만
        ReceiptAnalysisStatsdto stats = receiptResultService.getAnalysisPageStats(uId);
        model.addAttribute("genderData", stats.getGenderData());
        model.addAttribute("myAvg", stats.getMyAvg());
        model.addAttribute("allAvg", stats.getAllAvg());
    }
}
