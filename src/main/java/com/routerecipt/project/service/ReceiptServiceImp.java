package com.routerecipt.project.service;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.routerecipt.project.OpenAI.OpenAiOcrAssisService;
import com.routerecipt.project.OpenAI.ReceiptAutoItemHelper;
import com.routerecipt.project.dto.ItemCategory;
import com.routerecipt.project.dto.ReceiptDTO;
import com.routerecipt.project.dto.ReceiptItemDTO;
import com.routerecipt.project.dto.UploadResult;
import com.routerecipt.project.ocr.OcrService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReceiptServiceImp implements ReceiptService {

    private final OcrService ocrService;
    private final OpenAiOcrAssisService openAiOcrAssisService; // ✅ OCR 보조
    private final ReceiptAutoItemHelper receiptAutoItemHelper; // ✅ 강제 아이템
    private final ReceiptApplicationService receiptApplicationService;

    @Override
    public UploadResult uploadReceipts(List<MultipartFile> files, String userId) {

        UploadResult result = new UploadResult();
        List<Long> successNos = new ArrayList<>();

        int success = 0;
        int fail = 0;

        for (MultipartFile file : files) {
            try {
                /* =====================
                 * 1️⃣ OCR 1차
                 * ===================== */
                if (file == null || file.isEmpty()) {
                    fail++;
                    continue;
                }

                JSONObject json = ocrService.callClovaOCR(file);
                if (json == null) {
                    fail++;
                    continue;
                }

                ReceiptDTO receipt = ocrService.parseReceiptWithAssist(json, file);
                if (receipt == null) {
                    fail++;
                    continue;
                }

                receipt.setR_u(userId);

                /* =====================
                 * 2️⃣ OCR 누락 보조 (OpenAI)
                 * ===================== */
                openAiOcrAssisService.assistIfNeeded(file, "");
                // 👉 날짜 / 장소 / 포맷 등만 보완

                /* =====================
                 * 3️⃣ 자동 아이템 RULE
                 * ===================== */
                receiptAutoItemHelper.applyIfNeeded(receipt);

                /* =====================
                 * 4️⃣ DB 무결성 보호
                 * ===================== */
                applyFinalFallback(receipt);

                receiptApplicationService.saveReceiptWithItems(receipt);

                successNos.add(receipt.getR_no());
                success++;

            } catch (Exception e) {
                e.printStackTrace();
                fail++;
            }
        }

        result.setSuccessReceiptNos(successNos);
        result.setSuccessCount(success);
        result.setFailCount(fail);
        return result;
    }

    /**
     * DB NOT NULL 보호 (item_category)
     */
    private void applyFinalFallback(ReceiptDTO receipt) {

        if (receipt.getItems() == null) return;

        for (ReceiptItemDTO item : receipt.getItems()) {
            if (item == null) continue;

            if (item.getItem_category() == null) {
                item.setItem_category(ItemCategory.ETC.name());
                item.setAi_source("FALLBACK");
                item.setAi_confidence(0.0);
            }
        }
    }
}
