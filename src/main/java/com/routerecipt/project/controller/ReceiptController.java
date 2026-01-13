package com.routerecipt.project.controller;


import java.security.Principal;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.routerecipt.project.dto.ReceiptDTO;
import com.routerecipt.project.service.ReceiptQueryService;
import com.routerecipt.project.service.ReceiptService;
import com.routerecipt.project.stream.OcrStreamProducer;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


/**
 * 영수증 관련 MVC Controller
 *
 * Base URL: /receipt
 *
 * 담당 기능:
 *  - 영수증 이미지 업로드(OCR 분석) 및 저장
 *  - 최근 분석한 영수증 목록 조회(세션 기반)
 *  - 수기 영수증 저장
 *  - OCR 결과 확정(수정 반영)
 */
@Slf4j
@Controller
@RequestMapping("/receipt")
@RequiredArgsConstructor
public class ReceiptController {

    private final ReceiptService receiptService;
    private final ReceiptQueryService receiptQueryService;
    private final OcrStreamProducer ocrStreamProducer;
    private final ObjectMapper objectMapper;

    // =========================
    // 1️⃣ 비동기 OCR 업로드
    // =========================
    @PostMapping("/uploadReceipt")
    public String uploadReceipt(
            @RequestParam("receipt") List<MultipartFile> files,
            Principal principal,
            HttpSession session,
            RedirectAttributes ra
    ) {
        if (principal == null) return "redirect:/login";

        String userId = principal.getName();
        List<Long> receiptIds =
        	    receiptService.uploadReceipts(files, userId)
        	                  .getSuccessReceiptNos();

        	log.info("[UPLOAD] 생성된 receiptIds = {}", receiptIds);

        	// 🔥 핵심: receipt별 OCR 이벤트 발행
        	for (Long receiptNo : receiptIds) {
        	    String imagePath =
        	        receiptService.getImagePathByReceiptNo(receiptNo);

        	    log.info(
        	        "[UPLOAD] publish OCR event r_no={}, imagePath={}",
        	        receiptNo,
        	        imagePath
        	    );

        	    ocrStreamProducer.publishOcrEvent(
        	        userId,
        	        imagePath,
        	        receiptNo
        	    );
        	}
    // =========================
    // 2️⃣ 영수증 등록 페이지 (조회 전용)
    // =========================
    @GetMapping("/receiptRegisterPage")
    public String receiptRegisterPage(
            Principal principal,
            HttpSession session,
            Model model
    ) throws JsonProcessingException {

        if (principal == null) return "redirect:/login";

        @SuppressWarnings("unchecked")
        List<Long> receiptIds =
            (List<Long>) session.getAttribute("CURRENT_RECEIPT_IDS");

        // ✅ 추가 로그 (핵심)
        log.info("[PAGE] 세션 CURRENT_RECEIPT_IDS = {}", receiptIds);

        List<ReceiptDTO> receipts = Collections.emptyList();

        if (receiptIds != null && !receiptIds.isEmpty()) {
            receipts = receiptQueryService.getRecentReceipts(receiptIds);

            // ✅ 추가 로그
            log.info("[PAGE] 조회된 receipts size = {}", receipts.size());
        } else {
            log.warn("[PAGE] receiptIds 없음 → 조회 안함");
        }

        model.addAttribute("receipts", receipts);
        model.addAttribute(
            "receiptsJson",
            objectMapper.writeValueAsString(receipts)
        );

        return "receipt/receiptRegisterPage";
    }


}
