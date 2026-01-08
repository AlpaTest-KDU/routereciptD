package com.routerecipt.project.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.routerecipt.project.dto.ReceiptDTO;


/**
 * 영수증(Receipt) 도메인의 애플리케이션 서비스 인터페이스
 *
 * - 컨트롤러(화면/요청) 기준의 유즈케이스 단위를 정의한다.
 * - 저장/확정(Write) + 조회/화면구성(Read) 기능을 포함한다.
 * - 구현체에서는 Mapper/Redis/AI/OCR 등의 하위 컴포넌트를 조합해 처리하는 위치가 된다.
 */
public interface ReceiptApplicationService {

    // Write(저장/확정)
	// 영수증(헤더) + 영수증 아이템 목록을 함께 저장한다.
    void saveReceiptWithItems(ReceiptDTO receipt);

    // 임시(TEMP) 상태의 영수증을 사용자가 수정/확정할 때 호출되는 메서드
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
