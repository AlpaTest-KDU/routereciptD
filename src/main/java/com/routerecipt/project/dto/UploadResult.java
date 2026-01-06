package com.routerecipt.project.dto;

import java.util.List;

import lombok.Getter;
import lombok.Setter;


/**
 * 영수증 업로드 처리 결과 DTO
 *
 * 역할:
 *  - 여러 장의 영수증 업로드(OCR + 저장) 결과를 요약해서 전달
 *  - 성공/부분 성공/전체 실패 상태 판단에 사용
 */
@Getter
@Setter
public class UploadResult {

    private List<Long> successReceiptNos;	// 성공적으로 처리된 영수증 번호 목록
    private int successCount;				// 성공한 영수증 개수
    private int failCount;					// 실패한 영수증 개수

    
    /**
     * 모든 영수증 처리 실패 여부
     *
     * @return 성공 건수가 0이면 true
     */
    public boolean isAllFailed() {
        return successCount == 0;
    }

    
    /**
     * 일부 성공 여부
     *
     * @return 성공과 실패가 동시에 존재하면 true
     */
    public boolean isPartialSuccess() {
        return successCount > 0 && failCount > 0;
    }
}
