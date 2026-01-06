package com.routerecipt.project.service;

import org.json.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.routerecipt.project.dto.ReceiptDTO;
import com.routerecipt.project.mapper.ReceiptMapper;
import com.routerecipt.project.ocr.OcrService;

import lombok.RequiredArgsConstructor;


/**
 * 영수증 분석 서비스 구현체
 *
 * - 업로드된 영수증 파일을 Clova OCR로 분석한다.
 * - OCR 결과를 파싱하여 ReceiptDTO + Item 목록을 생성한다.
 * - 분석 결과를 DB에 "임시(TEMP)"로 저장하고 영수증 번호(rNo)를 반환한다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ReceiptanalyzeServiceImp implements ReceiptAnalyzeService {
	
	 // 영수증/아이템 DB 저장용 Mapper
    private final ReceiptMapper receiptMapper;
    
    // OCR 호출 및 OCR 결과 파싱 담당 서비스
    private final OcrService ocrService;

    // 영수증 파일을 분석(OCR)하고 임시 저장한 뒤 영수증 번호를 반환
    @Override
    public Long analyzeReceipt(MultipartFile file, String userId) {
    	
    	// 0) 파일 유효성 검사
        if (file == null || file.isEmpty()) {
            return null;
        }

        // 1) OCR 호출 (Clova OCR)
        JSONObject json = ocrService.callClovaOCR(file);
        if (json == null) return null;
        
        // 2) OCR 결과 파싱 + Assist 처리
        // - json + file 정보를 기반으로 ReceiptDTO(영수증 + 아이템 목록)를 생성
        ReceiptDTO temp =
                ocrService.parseReceiptWithAssist(json, file);
        if (temp == null) return null;

        // 3) 임시 영수증 저장 준비
        // - 영수증의 소유 사용자 지정
        temp.setR_u(userId);
        // ⚠️ category TEMP 제거 (A 방향)
        
        // 4) 영수증(헤더) 저장
        receiptMapper.insertReceipt(temp);
        Long rNo = temp.getR_no();

        // 5) 영수증 아이템 저장
        // - 영수증 번호가 존재하고, 아이템 목록이 있으면 저장
        if (rNo != null && temp.getItems() != null && !temp.getItems().isEmpty()) {
        	
        	 // 각 아이템에 영수증 번호 FK 세팅
            temp.getItems().forEach(it -> it.setR_no(rNo));
            
            // 아이템 목록 일괄 저장
            receiptMapper.insertReceiptItems(rNo, temp.getItems());
        }
        
        // 6) 저장된 영수증 번호 반환
        return rNo;
    }
}
