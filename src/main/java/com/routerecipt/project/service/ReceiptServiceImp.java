package com.routerecipt.project.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.routerecipt.project.dto.ReceiptDTO;
import com.routerecipt.project.mapper.ReceiptMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReceiptServiceImp implements ReceiptService {
	
	@Autowired
	private ReceiptMapper receiptMapper;
	
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
	
}
