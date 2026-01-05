package com.routerecipt.project.controller;


import java.security.Principal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.routerecipt.project.dto.ReceiptDTO;
import com.routerecipt.project.dto.UploadResult;
import com.routerecipt.project.service.ReceiptAnalyzeService;
import com.routerecipt.project.service.ReceiptQueryService;
import com.routerecipt.project.service.ReceiptService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequestMapping("/receipt")
@RequiredArgsConstructor
public class ReceiptController {
	
	private final ReceiptService receiptService;
	private final ReceiptQueryService receiptQueryService;
	private final ReceiptAnalyzeService receiptAnalyzeService;
	
	// 영수증 등록 기능
	@PostMapping("/receiptRegisterPage/ReceiptRegister")
	public String receiptRegister() {
		return "receiptRegisterPage";
	}
	
	// 영수증 삭제 기능
	@DeleteMapping("/userInfoShowPage/receiptInfoDelete/{receiptNo}")
	public String receiptInfoDelete(@PathVariable(value="receiptNo") Long receiptNo) {
		return "userInfoShowPage";
	}
	
	@PostMapping("/userInfoShowPage/receiptInfoShow")
	public String receiptInfoShow() {
		return "userInfoShowPage";
	}


	 @PostMapping("/uploadReceipt")
	    public String uploadReceipt(
	            @RequestParam("receipt") List<MultipartFile> files,
	            Principal principal,
	            HttpSession session,
	            RedirectAttributes ra) {

	        // 1️⃣ 로그인 체크
	        if (principal == null) {
	            return "redirect:/login";
	        }

	        // 2️⃣ 파일 유효성 체크
	        if (files == null || files.isEmpty()) {
	            ra.addFlashAttribute("saveMsg", "파일을 선택해 주세요.");
	            return "redirect:/receipt/receiptRegisterPage";
	        }


	        // 3️⃣ Service 호출 (OCR + 저장 + 부분 성공 처리)
	        UploadResult result =
	                receiptService.uploadReceipts(files, principal.getName());

	        // 4️⃣ 결과 분기
	        if (result.isAllFailed()) {
	            ra.addFlashAttribute("saveMsg", "모든 영수증 분석에 실패했습니다.");
	            return "redirect:/receipt/receiptRegisterPage";
	        }

	        // 성공한 영수증 번호 세션 저장
	        session.setAttribute("recentReceiptNos",
	                result.getSuccessReceiptNos());

	        if (result.isPartialSuccess()) {
	            ra.addFlashAttribute(
	                "saveMsg",
	                "일부 영수증만 분석되었습니다. (성공 "
	                + result.getSuccessCount() + "건)"
	            );
	        } else {
	            ra.addFlashAttribute("saveMsg", "영수증 분석이 완료되었습니다.");
	        }

	        return "redirect:/receipt/receiptRegisterPage";
	    }

    // =========================
    // 2) 페이지 조회: "이번에 분석한 것만" 보여주기
    // =========================
	 @GetMapping("/receiptRegisterPage")
	 public String receiptRegisterPage(Principal principal,
	                                   HttpSession session,
	                                   Model model) {

	     if (principal == null) return "redirect:/login";

	     @SuppressWarnings("unchecked")
	     List<Long> recentNos =
	             (List<Long>) session.getAttribute("recentReceiptNos");

	     if (recentNos == null || recentNos.isEmpty()) {
	         model.addAttribute("receipts", Collections.emptyList());
	         return "receipt/receiptRegisterPage";
	     }

	     List<ReceiptDTO> receipts =
	             receiptQueryService.getRecentReceipts(recentNos);

	     model.addAttribute("receipts", receipts);
	     return "receipt/receiptRegisterPage";
	 }

	 @GetMapping("/writeReceipt")
	 public String writeReceiptForm(Model model, Principal principal) {

	     if (principal == null) {
	         return "redirect:/login";
	     }

	     ReceiptDTO receipt = new ReceiptDTO();
	     receipt.setR_u(principal.getName());
	     receipt.setR_place("");
	     receipt.setR_date(LocalDate.now());

	     model.addAttribute("receipt", receipt);
	     return "home";
	 }
    
	 @PostMapping("/analyze")
	 public String analyze(@RequestParam("receipt") MultipartFile file,
	                       Principal principal,
	                       HttpSession session,
	                       RedirectAttributes ra) {

	     if (principal == null) {
	         return "redirect:/login";
	     }

	     Long r_no =
	             receiptAnalyzeService.analyzeReceipt(
	                 file,
	                 principal.getName()
	             );

	     if (r_no == null) {
	         ra.addFlashAttribute("saveMsg", "OCR 결과를 만들지 못했습니다.");
	         return "redirect:/receipt";
	     }

	     @SuppressWarnings("unchecked")
	     List<Long> recent =
	             (List<Long>) session.getAttribute("recentReceiptNos");

	     if (recent == null) {
	         recent = new ArrayList<>();
	     }
	     recent.add(r_no);
	     session.setAttribute("recentReceiptNos", recent);

	     return "redirect:/receipt/receiptRegisterPage";
	 }


}
