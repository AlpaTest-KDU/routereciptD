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
    
	// 영수증 파일 목록 업로드 및 처리
    UploadResult uploadReceipts(List<MultipartFile> files, String userId);
}
