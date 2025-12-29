package com.routerecipt.project.service;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.routerecipt.project.dto.UploadResult;

public interface ReceiptService {
    
    UploadResult uploadReceipts(List<MultipartFile> files, String userId);
}
