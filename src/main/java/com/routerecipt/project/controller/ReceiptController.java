package com.routerecipt.project.controller;


import java.security.Principal;
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
            RedirectAttributes ra
    ) {
        if (principal == null) {
            return "redirect:/login";
        }

        if (files == null || files.isEmpty()) {
            ra.addFlashAttribute("saveMsg", "파일을 선택해 주세요.");
            return "redirect:/receipt/receiptRegisterPage";
        }

        String userId = principal.getName();

        // ✅ 1) 파일 임시 저장
        List<String> imagePaths = receiptService.saveTempFiles(files);

        // ✅ 2) Redis Stream에 OCR 이벤트 발행
        for (String path : imagePaths) {
            ocrStreamProducer.publishOcrEvent(userId, path);
            log.info("[OCR-ASYNC] publish userId={}, path={}", userId, path);
        }

        ra.addFlashAttribute("saveMsg", "영수증 분석을 시작했습니다.");
        return "redirect:/receipt/receiptRegisterPage";
    }

    // =========================
    // 2️⃣ 영수증 등록 페이지 (조회 전용)
    // =========================
    @GetMapping("/receipt/receiptRegisterPage")
    public String receiptRegisterPage(Principal principal,
                                      Model model) throws JsonProcessingException {

        if (principal == null) return "redirect:/login";

        String userId = principal.getName();

        // ✅ DB 기준 최근 영수증 조회 (비동기 대응)
        List<ReceiptDTO> receipts =
                receiptQueryService.getRecentReceiptsByUser(userId, 20);

        String receiptsJson =
                objectMapper.writeValueAsString(receipts);

        model.addAttribute("receipts", receipts);
        model.addAttribute("receiptsJson", receiptsJson);

        return "receipt/receiptRegisterPage";
    }
}
