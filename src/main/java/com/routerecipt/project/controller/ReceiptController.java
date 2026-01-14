package com.routerecipt.project.controller;


import java.security.Principal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.routerecipt.project.dto.ReceiptDTO;
import com.routerecipt.project.service.ReceiptCommandService;
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
    private final ReceiptCommandService receiptCommandService;

    // =========================
    // 1️⃣ 비동기 OCR 업로드
    // =========================
    @PostMapping("/uploadReceipt")
    @ResponseBody
    public ResponseEntity<Void> uploadReceipt(
            @RequestParam("receipt") List<MultipartFile> files,
            Principal principal,
            HttpSession session
    ) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }

        String userId = principal.getName();

        List<Long> receiptIds =
            receiptService.uploadReceipts(files, userId)
                          .getSuccessReceiptNos();

        log.info("[UPLOAD] 생성된 receiptIds = {}", receiptIds);

        for (Long receiptNo : receiptIds) {
            String imagePath =
                receiptService.getImagePathByReceiptNo(receiptNo);

        }

        session.setAttribute("CURRENT_RECEIPT_IDS", receiptIds);

        // 🔥 핵심: redirect ❌, JSON ❌
        return ResponseEntity.noContent().build(); // 204
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
    
    @GetMapping("/polling")
    @ResponseBody
    public List<ReceiptDTO> pollReceipts(
            HttpSession session,
            Principal principal
    ) {
        if (principal == null) {
            return Collections.emptyList();
        }

        @SuppressWarnings("unchecked")
        List<Long> receiptIds =
            (List<Long>) session.getAttribute("CURRENT_RECEIPT_IDS");

        if (receiptIds == null || receiptIds.isEmpty()) {
            return Collections.emptyList();
        }

        return receiptQueryService.getRecentReceipts(receiptIds);
    }

    @PostMapping("/confirm")
    public String confirmReceipt(
            @RequestParam Long r_no,
            @RequestParam String r_place,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate r_date,
            @RequestParam Integer r_price,
            @RequestParam List<String> item_names,
            @RequestParam List<Integer> item_prices,
            @RequestParam List<String> item_categories
    ) {
        receiptCommandService.confirmReceipt(
            r_no,
            r_place,
            r_date,
            r_price,
            item_names,
            item_prices,
            item_categories
        );

        return "redirect:/receipt/receiptRegisterPage";
    }


}
