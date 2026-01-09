package com.routerecipt.project.controller;

import com.routerecipt.project.dto.CalendarDTO;
import com.routerecipt.project.security.LoginDetails;
import com.routerecipt.project.service.ReceiptQueryServiceImp;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.YearMonth;

@Controller
@RequiredArgsConstructor
public class CalendarController {

    private final ReceiptQueryServiceImp receiptQueryServiceImp;

    @GetMapping("/user/userInfoShowPage/calendar")
    @ResponseBody
    public CalendarDTO makeCalendar(Authentication authentication,
                                    @RequestParam(required = false) Integer year,
                                    @RequestParam(required = false) Integer month) {
        
        LoginDetails loginDetails = (LoginDetails) authentication.getPrincipal();
        String u_id = loginDetails.getUser().getU_id();
        
        CalendarDTO calendar = new CalendarDTO();
        String currentYearMonth = YearMonth.now().toString();
        
        if (year != null && month != null) {
            currentYearMonth = String.format("%d-%02d", year, month);
        }
        calendar.setMonth(YearMonth.parse(currentYearMonth));
        calendar.setReceipt(receiptQueryServiceImp.getSavedReceiptsDate(u_id, currentYearMonth));
        return calendar;
    }
}
