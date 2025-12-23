package com.routerecipt.project.ocr;

import org.json.JSONObject;

import com.routerecipt.project.dto.ReceiptDTO;
import com.routerecipt.project.dto.Userdto;

public interface Service {
	void signup (Userdto u);
	void save(ReceiptDTO dto);
	ReceiptDTO parseReceiptWithAssist(JSONObject json);
}


