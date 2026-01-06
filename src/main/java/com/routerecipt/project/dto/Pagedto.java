package com.routerecipt.project.dto;

import lombok.Getter;


/**
 * 페이지네이션 계산 DTO
 *
 * 역할:
 *  - Criteria(페이지 요청 정보)와 total(전체 데이터 수)를 기반으로
 *    페이지 번호 UI에 필요한 정보 계산
 *
 * 페이지 번호 정책:
 *  - 한 블록에 10페이지씩 표시 (1~10, 11~20, ...)
 */
@Getter
public class Pagedto {
    private int startPage;		// 페이지 블록 시작 번호
    private int endPage;		// 페이지 블록 끝 번호
    private boolean prev;		// 이전 페이지 블록 존재 여부
    private boolean next;		// 다음 페이지 블록 존재 여부
    private int total;			// 전체 데이터 개수
    private Criteria criteria;	// 현재 페이지 요청 정보

    
    /**
     * 페이지네이션 정보 생성자
     *
     * @param criteria 현재 페이지 정보
     * @param total    전체 데이터 개수
     */
    public Pagedto(Criteria criteria, int total) {
        this.criteria = criteria;
        this.total = total;

        // 끝 페이지 계산
        // 한 페이지당 10개씩
        // 1️⃣ 현재 페이지가 속한 페이지 블록의 끝 페이지 계산
        this.endPage = (int) (Math.ceil(criteria.getPageNum() / 10.0)) * 10;
        
        // 2️⃣ 시작 페이지 계산
        this.startPage = this.endPage - 9;

        // 3️⃣ 실제 마지막 페이지 계산
        int realEndPage = (int) (Math.ceil((total * 1.0) / criteria.getAmount()));
        
        // 4️⃣ 실제 마지막 페이지가 현재 endPage보다 작으면 보정
        if (realEndPage < this.endPage) {
            this.endPage = realEndPage;
        }
        
        // 5️⃣ 이전/다음 버튼 활성화 여부
        this.prev = this.startPage > 1;
        this.next = this.endPage < realEndPage;
    }
}