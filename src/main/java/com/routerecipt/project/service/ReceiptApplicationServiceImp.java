package com.routerecipt.project.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.routerecipt.project.dto.AiCategoryResponse;
import com.routerecipt.project.dto.AiTrainingItemDTO;
import com.routerecipt.project.dto.ReceiptDTO;
import com.routerecipt.project.dto.ReceiptItemDTO;

import lombok.RequiredArgsConstructor;


/**
 * 영수증(Receipt) 애플리케이션 서비스 구현체
 *
 * - 화면/요청 단위 유즈케이스를 조립하는 계층
 * - Write(저장/확정)는 CommandService에 위임하고, 필요 시 AI 분류/학습데이터 저장을 함께 수행
 * - Read(조회/달력/메뉴맵/최근 조회)는 QueryService에 위임
 */

@Service
@RequiredArgsConstructor
public class ReceiptApplicationServiceImp implements ReceiptApplicationService {


    private final ReceiptCommandService receiptCommandService;
    private final ReceiptQueryService receiptQueryService;
    private final AiCategoryService aiCategoryService;
    private final AiTrainingItemService aiTrainingItemService;
    private static final Logger log = LoggerFactory.getLogger(ReceiptApplicationServiceImp.class);


    /* ===============================
     * Write
     * =============================== */
    // 영수증 + 아이템 저장
    @Override
    public void saveReceiptWithItems(ReceiptDTO receipt) {

        Long rNo = receipt.getR_no();
        String userId = receipt.getR_u();
        int itemCount = (receipt.getItems() == null ? 0 : receipt.getItems().size());

        long totalStart = System.currentTimeMillis();

        log.info("[APP] SAVE START r_no={}, userId={}, itemCount={}",
                rNo, userId, itemCount);

        // =========================
        // 1️⃣ 아이템별 AI 카테고리 분류
        // =========================
        if (receipt.getItems() != null) {
            for (ReceiptItemDTO item : receipt.getItems()) {

                String itemName = item.getItem_name();
                long tAi = System.currentTimeMillis();

                log.info("[AI] CLASSIFY START r_no={}, item={}", rNo, itemName);

                try {
                    AiCategoryResponse ai =
                            aiCategoryService.classify(itemName);

                    // 2️⃣ 실제 저장 필드에 세팅 (snake_case)
                    item.setItem_category(ai.getCategory());
                    item.setAi_confidence(ai.getConfidence());
                    item.setAi_source(ai.getSource());

                    log.info("[AI] CLASSIFY END r_no={}, item={}, category={}, source={}, elapsed={}ms",
                            rNo,
                            itemName,
                            ai.getCategory(),
                            ai.getSource(),
                            System.currentTimeMillis() - tAi);

                } catch (Exception e) {
                    // ⭐ AI 실패 시에도 저장은 진행되도록 FALLBACK
                    log.warn("[AI] CLASSIFY FAIL r_no={}, item={}, fallback=ETC",
                            rNo, itemName, e);

                    item.setItem_category("ETC");
                    item.setAi_confidence(0.0);
                    item.setAi_source("FALLBACK");
                }
            }
        }

        // =========================
        // 3️⃣ DB 저장
        // =========================
        long tDb = System.currentTimeMillis();
        log.info("[DB] SAVE START r_no={}, itemCount={}", rNo, itemCount);

        receiptCommandService.saveReceiptWithItems(receipt);

        log.info("[DB] SAVE END r_no={}, elapsed={}ms",
                rNo, System.currentTimeMillis() - tDb);

        log.info("[APP] SAVE END r_no={}, totalElapsed={}ms",
                rNo, System.currentTimeMillis() - totalStart);
    }

    // 영수증 확정 처리
    @Override
    @Transactional
    public void confirmReceipt(
            Long r_no,
            String r_place,
            LocalDate r_date,
            Integer r_price,
            List<String> item_names,
            List<Integer> item_prices,
            List<String> item_categories
    ) {
        // 1️⃣ 기존 영수증 / 아이템 확정 처리
        receiptCommandService.confirmReceipt(
                r_no, r_place, r_date, r_price,
                item_names, item_prices, item_categories
        );

        // 2️⃣ 학습 데이터 저장 (여기 추가)
        for (int i = 0; i < item_names.size(); i++) {

            AiTrainingItemDTO ai = new AiTrainingItemDTO();
            ai.setItem_text(item_names.get(i));

            // 현재 구조상 final_label은 item_categories
            ai.setFinal_label(item_categories.get(i));

            // 예측값이 있다면 세팅 (없으면 null 허용)
            ai.setPredicted_label(null);   // or 기존 AI 결과
            ai.setAi_source("AI");
            ai.setModel_version("v1");

            aiTrainingItemService.saveTrainingItem(ai);
        }
    }

    /* ===============================
     * Read
     * =============================== */
    // 특정 사용자/월(yyyyMM)의 영수증 목록 조회
    @Override
    public List<ReceiptDTO> getSavedReceiptsDate(String userId, String yearMonth) {
        return receiptQueryService.getSavedReceiptsDate(userId, yearMonth);
    }

    // 영수증 목록을 기반으로 화면 출력용 메뉴 맵 구성
    @Override
    public Map<String, List<String>> buildMenuMap(List<ReceiptDTO> receipts) {
        return receiptQueryService.buildMenuMap(receipts);
    }

    // 달력 UI 구성을 위한 날짜/칸 데이터 생성
    @Override
    public List<Integer> buildCalendar(String yearMonth) {
        return receiptQueryService.buildCalendar(yearMonth);
    }
    
    // 최근(임시/TEMP) 영수증 목록 조회
    @Override
    public List<ReceiptDTO> getRecentReceipts(List<Long> r_no) {
        return receiptQueryService.getRecentReceipts(r_no);
    }

    @Override
    @Transactional
    public void updateOcrStatus(Long r_no, String status) {

        if (r_no == null || status == null) {
            log.warn("[OCR-STATUS] update skipped r_no={}, status={}", r_no, status);
            return;
        }

        log.info("[OCR-STATUS] UPDATE r_no={}, status={}", r_no, status);

        receiptCommandService.updateOcrStatus(r_no, status);
    }

    @Override
    @Transactional
    public void updateOcrStatusByImagePath(String imagePath, String status) {

        if (imagePath == null || imagePath.isBlank() || status == null) {
            log.warn("[OCR-STATUS] updateByPath skipped imagePath={}, status={}",
                     imagePath, status);
            return;
        }

        log.info("[OCR-STATUS] UPDATE imagePath={}, status={}", imagePath, status);

        receiptCommandService.updateOcrStatusByImagePath(imagePath, status);
    }
}
