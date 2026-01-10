package com.routerecipt.project.service;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.routerecipt.project.dto.UploadResult;


/**
 * 영수증 업로드 처리 서비스 인터페이스
 *
 * - 여러 장의 영수증 파일 업로드를 처리하는 상위 서비스
 * - 각 파일에 대한 분석/저장 결과를 종합하여 UploadResult로 반환한다.
 * - 컨트롤러는 이 인터페이스만 호출하면 된다.
 */
public interface ReceiptService {
	/**
     * ✅ 업로드된 파일을 임시 경로에 저장
     * - OCR 수행 ❌
     * - DB 저장 ❌
     * - Stream 발행 ❌
     *
     * @return 저장된 이미지 경로 목록
     */
    List<String> saveTempFiles(List<MultipartFile> files);
    
    /**
     * ✅ 임시 receipt(PENDING) 생성
     * - OCR ❌
     * - item 생성 ❌
     * - 상태만 PENDING
     *
     * @return 생성된 receipt_id 목록
     */
    

    /**
     * ❌ (비동기 전환 후 사용 중단 예정)
     * @deprecated 동기 OCR 구조용 메서드
     */
    
    UploadResult uploadReceipts(List<MultipartFile> files, String userId);
    
    
}
