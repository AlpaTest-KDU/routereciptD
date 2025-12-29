package com.routerecipt.project.service;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.routerecipt.project.dto.ReceiptDTO;
import com.routerecipt.project.dto.UploadResult;
import com.routerecipt.project.ocr.OcrService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
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

	            // ✅ 여기서 끝
	            receiptApplicationService.saveReceiptWithItems(receipt);

	            successNos.add(receipt.getR_no());
	            success++;

	        } catch (Exception e) {
	            fail++;
	        }
	    }

	    result.setSuccessReceiptNos(successNos);
	    result.setSuccessCount(success);
	    result.setFailCount(fail);

	    return result;
	}

}
	

