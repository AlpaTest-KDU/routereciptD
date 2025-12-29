package com.routerecipt.project.service;

import java.util.*;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.routerecipt.project.dto.ReceiptDTO;
import com.routerecipt.project.dto.ReceiptItemDTO;
import com.routerecipt.project.mapper.ReceiptMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReceiptQueryServiceImp implements ReceiptQueryService {

    private final ReceiptMapper receiptMapper;

    @Override
    public List<ReceiptDTO> getRecentReceipts(List<Long> receiptNos) {

        if (receiptNos == null || receiptNos.isEmpty()) {
            return Collections.emptyList();
        }

        // 1) receipt 조회
        List<ReceiptDTO> receipts =
                receiptMapper.selectTempReceiptsByNos(receiptNos);

        if (receipts == null || receipts.isEmpty()) {
            return Collections.emptyList();
        }

        // 2) items 일괄 조회
        List<ReceiptItemDTO> items =
                receiptMapper.selectItemsByReceiptNos(receiptNos);

     // 3) r_no 기준 grouping  ✅ (여기 블록을 교체)
        Map<Long, List<ReceiptItemDTO>> itemMap;
        if (items == null || items.isEmpty()) {
            itemMap = Collections.emptyMap();
        } else {
            itemMap = items.stream()
                    .filter(Objects::nonNull)          // item 자체 null 방지
                    .filter(it -> it.getR_no() != null) // ✅ groupingBy 키 null 방지 (NPE 원인)
                    .collect(Collectors.groupingBy(ReceiptItemDTO::getR_no));
        }

        for (ReceiptDTO r : receipts) {
            r.setItems(itemMap.getOrDefault(r.getR_no(), Collections.emptyList()));
        }

        // 4) 업로드 순서 유지 정렬
        Map<Long, Integer> order = new HashMap<>();
        for (int i = 0; i < receiptNos.size(); i++) {
            order.put(receiptNos.get(i), i);
        }

        receipts.sort(
            Comparator.comparingInt(
                r -> order.getOrDefault(r.getR_no(), Integer.MAX_VALUE)
            )
        );

        return receipts;
    }
}
