package com.routerecipt.project.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 페이징(Pagination) 기준 정보 DTO
 *
 * 역할:
 *  - 목록 조회 시 페이지 번호와 페이지당 개수를 관리
 *  - DB 조회를 위한 OFFSET 값을 계산
 *
 * 사용 예:
 *  Criteria cri = new Criteria(2, 10);
 *  cri.getSkip(); // 10
 */
@Getter
@Setter
public class Criteria {
	// 현재 페이지 번호 (1부터 시작)
    private int pageNum;
    // 페이지당 조회할 데이터 개수
    private int amount;

    /**
     * 기본 생성자
     *  - pageNum: 1
     *  - amount : 10
     */
    public Criteria() {
        this(1, 10);
    }
    
    
    /**
     * 생성자
     *
     * @param pageNum 현재 페이지 번호
     * @param amount 페이지당 데이터 개수
     */
    public Criteria(int pageNum, int amount) {
        this.pageNum = pageNum;
        this.amount = amount;
    }

    /**
     * SQL OFFSET 계산
     *
     * @return 건너뛸 데이터 개수
     */
    public int getSkip() {
        return (pageNum - 1) * amount;
    }
}