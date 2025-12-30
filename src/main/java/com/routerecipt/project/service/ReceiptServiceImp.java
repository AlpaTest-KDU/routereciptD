package com.routerecipt.project.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.json.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.routerecipt.project.dto.ReceiptDTO;
import com.routerecipt.project.dto.ReceiptItemDTO;
import com.routerecipt.project.dto.UploadResult;
import com.routerecipt.project.ocr.OcrService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReceiptServiceImp implements ReceiptService {

    private final OcrService ocrService;
    private final ReceiptApplicationService receiptApplicationService;

    @Override
    public UploadResult uploadReceipts(List<MultipartFile> files, String userId) {

        UploadResult result = new UploadResult();
        List<Long> successNos = new ArrayList<>();

        int success = 0;
        int fail = 0;

        for (MultipartFile file : files) {
            try {
                // 1) 파일 유효성
                if (file == null || file.isEmpty()) {
                    fail++;
                    continue;
                }

                // 2) OCR 호출
                JSONObject json = ocrService.callClovaOCR(file);
                if (json == null) {
                    fail++;
                    continue;
                }

                // 3) OCR 파싱 -> ReceiptDTO 생성
                ReceiptDTO receipt = ocrService.parseReceiptWithAssist(json, file);
                if (receipt == null) {
                    fail++;
                    continue;
                }

                // 4) 사용자 아이디 세팅
                receipt.setR_u(userId);

                // 5) 저장 직전에 "택시" 규칙 적용
                applyForcedItemsIfNeeded(receipt);

                // 6) 영수증 + 아이템 저장
                // (이 메서드 안에서 receipt insert → r_no 생성 → item insert 구조여야 함)
                receiptApplicationService.saveReceiptWithItems(receipt);

                successNos.add(receipt.getR_no());
                success++;

            } catch (Exception e) {
                // 로그 권장
                e.printStackTrace();
                fail++;
            }
        }

        result.setSuccessReceiptNos(successNos);
        result.setSuccessCount(success);
        result.setFailCount(fail);

        return result;
    }
    
 // =========================
 // 자동 아이템 추가 규칙 정의
 // =========================
    private static final List<ForcedItemRule> FORCED_ITEM_RULES = List.of(
	    new ForcedItemRule(
	        List.of("택시", "TAXI"),
	        "택시",
	        "TRAFFIC",
	        List.of("택시")          // item_name에 "택시" 포함이면 중복으로 간주 ("택시요금" 포함)
	    ),
	    new ForcedItemRule(
	        List.of("주차", "파킹", "PARKING"),
	        "주차",
	        "TRAFFIC",
	        List.of("주차", "파킹")   // "주차요금", "파킹요금" 등도 중복으로 간주
	    ),
	    new ForcedItemRule(
	        List.of("외과", "내과", "의원", "병원", "의학과", "비뇨기과"),
	        "진료비",
	        "MEDICAL",
	        List.of("진료", "진료비", "의료", "처방", "약") // 이미 "진료비"나 "진료"류가 있으면 추가 금지
	    )
	);

 
 private static class ForcedItemRule {
    private final List<String> placeKeywords;     // r_place 매칭용
    private final String itemName;                // 추가할 item_name
    private final String category;
    private final List<String> dedupKeywords;     // item_name 중복 판정용(그룹)

    public ForcedItemRule(List<String> placeKeywords, String itemName, String category, List<String> dedupKeywords) {
        this.placeKeywords = placeKeywords;
        this.itemName = itemName;
        this.category = category;
        this.dedupKeywords = dedupKeywords;
    }
}


	//=========================
	//r_place 기반 자동 아이템 추가 (확장 가능 구조)
	//=========================
 private void applyForcedItemsIfNeeded(ReceiptDTO receipt) {
	    String place = safe(receipt.getR_place());
	    if (place.isEmpty()) return;

	    if (receipt.getItems() == null) {
	        receipt.setItems(new ArrayList<>());
	    }

	    String upperPlace = place.toUpperCase();

	    for (ForcedItemRule rule : FORCED_ITEM_RULES) {

	        // 1) r_place 매칭
	        boolean matched = rule.placeKeywords.stream()
	                .anyMatch(k -> upperPlace.contains(k.toUpperCase()));

	        if (!matched) continue;

	        // 2) 룰별 중복 판정(그룹 중복 방지)
	        boolean alreadyInGroup = receipt.getItems().stream()
	                .filter(Objects::nonNull)
	                .map(ReceiptItemDTO::getItem_name)
	                .filter(Objects::nonNull)
	                .anyMatch(name -> rule.dedupKeywords.stream().anyMatch(dk -> name.contains(dk)));

	        if (alreadyInGroup) continue;

	        // 3) 추가
	        ReceiptItemDTO item = new ReceiptItemDTO();
	        item.setItem_name(rule.itemName);
	        item.setItem_category(rule.category);
	        item.setAi_source("RULE");
	        item.setAi_confidence(1.0);
	        item.setItem_price(receipt.getR_price());

	        receipt.getItems().add(item);
	    }
	}

	


    private String safe(String s) {
        return (s == null) ? "" : s.trim();
    }

    private Integer parsePriceOrNull(String priceStr) {
        if (priceStr == null) return null;
        String digits = priceStr.replaceAll("[^0-9]", "");
        if (digits.isBlank()) return null;
        try {
            return Integer.valueOf(digits);
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    
    
}
