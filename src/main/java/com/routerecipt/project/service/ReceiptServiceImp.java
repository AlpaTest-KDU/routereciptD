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
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReceiptServiceImp implements ReceiptService {

    private final OcrService ocrService;
    private final OpenAiOcrAssisService openAiOcrAssisService; // OCR 보조
    private final ReceiptAutoItemHelper receiptAutoItemHelper; // 강제 아이템
    private final ReceiptApplicationService receiptApplicationService;

    /**
     * 영수증 파일 업로드 메인 진입점
     *
     * 처리 흐름:
     * 1) 파일 유효성 검사
     * 2) Clova OCR 호출
     * 3) OCR 결과 파싱
     * 4) 사용자 ID 바인딩
     * 5) 장소 기반 자동 아이템 규칙 적용
     * 6) item_category NOT NULL 보정
     * 7) 영수증 + 아이템 저장
     */
    @Override
    public UploadResult uploadReceipts(List<MultipartFile> files, String userId) {

        UploadResult result = new UploadResult();
        List<Long> successNos = new ArrayList<>();

        int success = 0;
        int fail = 0;

        log.info("[UPLOAD] START userId={}, fileCount={}",
                userId, (files == null ? 0 : files.size()));

        for (MultipartFile file : files) {

            String fname = (file == null ? "null" : file.getOriginalFilename());
            long fileStart = System.currentTimeMillis();

            try {
                // 1️⃣ 파일 유효성
                if (file == null || file.isEmpty()) {
                    log.warn("[UPLOAD] FILE EMPTY name={}", fname);
                    fail++;
                    continue;
                }

                log.info("[UPLOAD] FILE START name={}, size={}",
                        fname, file.getSize());

                // 2️⃣ CLOVA OCR 호출
                long t1 = System.currentTimeMillis();
                log.info("[OCR] CLOVA CALL START name={}", fname);

                JSONObject json = ocrService.callClovaOCR(file);

                log.info("[OCR] CLOVA CALL END name={}, elapsed={}ms",
                        fname, System.currentTimeMillis() - t1);

                if (json == null) {
                    log.warn("[OCR] CLOVA RESULT NULL name={}", fname);
                    fail++;
                    continue;
                }

                // 3️⃣ OCR 파싱 + OpenAI 보강
                long t2 = System.currentTimeMillis();
                log.info("[OCR] PARSE+ASSIST START name={}", fname);

                ReceiptDTO receipt = ocrService.parseReceiptWithAssist(json, file);

                log.info("[OCR] PARSE+ASSIST END name={}, elapsed={}ms",
                        fname, System.currentTimeMillis() - t2);

                if (receipt == null) {
                    log.warn("[OCR] PARSE RESULT NULL name={}", fname);
                    fail++;
                    continue;
                }

                // 4️⃣ 사용자 아이디 세팅
                receipt.setR_u(userId);

                // 5️⃣ r_place 기반 강제 아이템 RULE 적용
                applyForcedItemsIfNeeded(receipt);

                // 6️⃣ DB 저장 전 item_category 최종 보정
                applyCategoryFallback(receipt);

                // 7️⃣ DB 저장
                long t3 = System.currentTimeMillis();
                log.info("[DB] SAVE START name={}", fname);

                receiptApplicationService.saveReceiptWithItems(receipt);

                log.info("[DB] SAVE END name={}, r_no={}, elapsed={}ms",
                        fname, receipt.getR_no(),
                        System.currentTimeMillis() - t3);

                successNos.add(receipt.getR_no());
                success++;

                log.info("[UPLOAD] FILE SUCCESS name={}, totalElapsed={}ms",
                        fname, System.currentTimeMillis() - fileStart);

            } catch (Exception e) {
                log.error("[UPLOAD] FILE FAIL name={}", fname, e);
                fail++;
            }
        }

        log.info("[UPLOAD] END userId={}, success={}, fail={}",
                userId, success, fail);

        result.setSuccessReceiptNos(successNos);
        result.setSuccessCount(success);
        result.setFailCount(fail);
        return result;
    }


    /**
     * item_category NOT NULL 보장을 위한 최종 방어 로직
     *
     * - 아이템이 존재하지만 category가 없을 경우
     * - ETC + FALLBACK + confidence 0으로 강제 보정
     * - DB 무결성 보장 목적
     */
    private void applyCategoryFallback(ReceiptDTO receipt) {

        if (receipt.getItems() == null || receipt.getItems().isEmpty()) {
            return;
        }

        for (ReceiptItemDTO item : receipt.getItems()) {
            if (item == null) continue;

            if (item.getItem_category() == null || item.getItem_category().isBlank()) {
                item.setItem_category(ItemCategory.ETC.name());
                item.setAi_source("FALLBACK");
                item.setAi_confidence(0.0);
            }
        }
    }

    // =========================
    // 자동 아이템 추가 규칙
    // =========================

    /**
     * 장소명(r_place)을 기반으로 자동 아이템을 추가하기 위한 규칙 집합
     *
     * - 교통(택시, 주차)
     * - 의료(병원, 의원 등)
     * - 중복 아이템 방지 키워드 포함
     */
    private static final List<ForcedItemRule> FORCED_ITEM_RULES = List.of(
        new ForcedItemRule(
            List.of("택시", "TAXI"),
            "택시",
            ItemCategory.TRAFFIC.name(),
            List.of("택시")
        ),
        new ForcedItemRule(
            List.of("주차", "파킹", "PARKING"),
            "주차",
            ItemCategory.TRAFFIC.name(),
            List.of("주차", "파킹")
        ),
        new ForcedItemRule(
            List.of("외과", "내과", "의원", "병원", "의학과", "비뇨기과"),
            "진료비",
            ItemCategory.MEDICAL.name(),
            List.of("진료", "진료비", "의료", "처방", "약")
        )
    );

    /**
     * 자동 아이템 규칙 단위 객체
     *
     * - placeKeywords : 장소 매칭 키워드
     * - itemName      : 추가할 아이템명
     * - category      : 아이템 카테고리
     * - dedupKeywords : 중복 방지 키워드
     */
    private static class ForcedItemRule {
        private final List<String> placeKeywords;
        private final String itemName;
        private final String category;
        private final List<String> dedupKeywords;

        public ForcedItemRule(
                List<String> placeKeywords,
                String itemName,
                String category,
                List<String> dedupKeywords) {

            this.placeKeywords = placeKeywords;
            this.itemName = itemName;
            this.category = category;
            this.dedupKeywords = dedupKeywords;
        }
    }

    /**
     * r_place 값을 기준으로 자동 아이템을 추가하는 로직
     *
     * - 장소 키워드 매칭
     * - 기존 아이템과 중복 여부 검사
     * - 조건 충족 시 RULE 기반 아이템 생성
     */
    private void applyForcedItemsIfNeeded(ReceiptDTO receipt) {

        String place = safe(receipt.getR_place());
        if (place.isEmpty()) return;

        if (receipt.getItems() == null) {
            receipt.setItems(new ArrayList<>());
        }

        String upperPlace = place.toUpperCase();

        for (ForcedItemRule rule : FORCED_ITEM_RULES) {

            boolean matched = rule.placeKeywords.stream()
                    .anyMatch(k -> upperPlace.contains(k.toUpperCase()));

            if (!matched) continue;

            boolean alreadyInGroup = receipt.getItems().stream()
                    .filter(item -> item != null && item.getItem_name() != null)
                    .anyMatch(item ->
                            rule.dedupKeywords.stream().anyMatch(item.getItem_name()::contains)
                    );

            if (alreadyInGroup) continue;

            ReceiptItemDTO item = new ReceiptItemDTO();
            item.setItem_name(rule.itemName);
            item.setItem_category(rule.category);
            item.setAi_source("RULE");
            item.setAi_confidence(1.0);
            item.setItem_price(receipt.getR_price());

            receipt.getItems().add(item);
        }
    }

    /**
     * null-safe 문자열 처리 유틸
     *
     * - null → ""
     * - 공백 trim
     */
    private String safe(String s) {
        return (s == null) ? "" : s.trim();
    }
}
