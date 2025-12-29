package com.routerecipt.project.service;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.springframework.web.multipart.MultipartFile;

import com.routerecipt.project.dto.ReceiptDTO;
import com.routerecipt.project.dto.UploadResult;

@Mapper
public interface ReceiptService {
	/**
     * 특정 사용자, 특정 월의 영수증 목록 조회
     * (receipt + user + receipt_item JOIN)
     */
    List<ReceiptDTO> getSavedReceiptsDate(String userId, String yearMonth);

    /**
     * 영수증 저장 (receipt + receipt_item)
     */
    void saveReceipt(ReceiptDTO receipt);
    
    UploadResult uploadReceipts(List<MultipartFile> files, String userId);
}
