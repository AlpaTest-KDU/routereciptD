package com.routerecipt.project.service;

import java.time.LocalDate;
import java.util.List;

import com.routerecipt.project.dto.ReceiptDTO;

public interface ReceiptCommandService {

    void saveReceiptWithItems(ReceiptDTO receipt);

    void confirmReceipt(
            Long r_no,
            String r_place,
            LocalDate r_date,
            Integer r_price,
            List<String> item_names,
            List<Integer> item_prices,
            List<String> item_categories
    );
}
