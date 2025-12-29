package com.routerecipt.project.service;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.routerecipt.project.dto.ItemCategory;
import com.routerecipt.project.dto.ReceiptDTO;
import com.routerecipt.project.dto.ReceiptItemDTO;
import com.routerecipt.project.dto.UploadResult;
import com.routerecipt.project.mapper.ReceiptMapper;
import com.routerecipt.project.ocr.OcrService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ReceiptServiceImp implements ReceiptService {
	
	
	private  final ReceiptMapper receiptMapper;
	
	private final OcrService ocrService;
	@Override
	public List<ReceiptDTO> getSavedReceiptsDate(String userId, String yearMonth) {
		return receiptMapper.getSavedReceiptsDate(userId, yearMonth);
	}
	
	@Override
	@Transactional
	public void saveReceipt(ReceiptDTO receipt) {
		
		receiptMapper.insertReceipt(receipt);
		
		if (receipt.getItems() != null && !receipt.getItems().isEmpty()) {
			receiptMapper.insertReceiptItems(
					receipt.getR_no(),
					receipt.getItems()
					);
		}
		
	}
	
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

                if (receipt.getItems() != null) {
                    for (ReceiptItemDTO it : receipt.getItems()) {
                        if (it.getItem_category() == null || it.getItem_category().isBlank()) {
                            it.setItem_category(ItemCategory.ETC.name());
                        }
                    }
                }

                receiptMapper.insertReceipt(receipt);
                Long rNo = receipt.getR_no();

                if (rNo != null && receipt.getItems() != null && !receipt.getItems().isEmpty()) {
                    for (ReceiptItemDTO it : receipt.getItems()) {
                        it.setR_no(rNo);
                    }
                    receiptMapper.insertReceiptItems(rNo, receipt.getItems());
                }

                successNos.add(rNo);
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
	

