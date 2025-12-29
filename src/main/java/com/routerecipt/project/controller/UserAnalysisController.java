package com.routerecipt.project.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.routerecipt.project.dto.Userdto; // ✅ loginUser DTO
import com.routerecipt.project.dto.ReceiptAnalysisStatsdto; // ✅ 변경된 DTO
import com.routerecipt.project.receipt.ReceiptResultService; // ✅ stats 가져오는 서비스(프로젝트 실제 경로로 유지)

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserAnalysisController {

    private final ReceiptResultService receiptResultService;

    @GetMapping("/analysisPage")
    public String analysisPage(HttpSession session, Model model) {

        // 1) 로그인 사용자 세션 확인
        Userdto loginUser = (Userdto) session.getAttribute("loginUser");
        if (loginUser == null) {
            return "redirect:/"; // ✅ "/"가 index로 매핑
        }

        // 2) 로그인 사용자 ID
        String uId = loginUser.getU_id();

        // 3) 서비스에서 내 지출 통계 조회 (✅ 타입 변경)
        ReceiptAnalysisStatsdto stats = receiptResultService.getStats(uId);

        // 4) 모델 바인딩
        model.addAttribute("dailyData",  stats.getDailyData());
        model.addAttribute("weeklyData", stats.getWeeklyData());
        model.addAttribute("monthlyData", stats.getMonthlyData());
        model.addAttribute("genderData", stats.getGenderData());
        model.addAttribute("myAvg",      stats.getMyAvg());
        model.addAttribute("allAvg",     stats.getAllAvg());

        // 5) view 반환: templates/user/analysisPage.html
        return "user/analysisPage";
    }
}
