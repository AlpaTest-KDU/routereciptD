package com.routerecipt.project.service;

import org.json.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.routerecipt.project.dto.ReceiptDTO;
import com.routerecipt.project.mapper.ReceiptMapper;
import com.routerecipt.project.ocr.OcrService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ReceiptanalyzeServiceImp implements ReceiptAnalyzeService {

    private final ReceiptMapper receiptMapper;
    private final OcrService ocrService;

    @Override
    public Long analyzeReceipt(MultipartFile file, String userId) {

        if (file == null || file.isEmpty()) {
            return null;
        }

        // 1) OCR
        JSONObject json = ocrService.callClovaOCR(file);
        if (json == null) return null;

        ReceiptDTO temp =
                ocrService.parseReceiptWithAssist(json, file);
        if (temp == null) return null;

        // 2) TEMP receipt 저장
        temp.setR_u(userId);
        // ⚠️ category TEMP 제거 (A 방향)
        receiptMapper.insertReceipt(temp);
        Long rNo = temp.getR_no();

        // 3) TEMP items 저장
        if (rNo != null && temp.getItems() != null && !temp.getItems().isEmpty()) {
            temp.getItems().forEach(it -> it.setR_no(rNo));
            receiptMapper.insertReceiptItems(rNo, temp.getItems());
        }

        return rNo;
    }
}
