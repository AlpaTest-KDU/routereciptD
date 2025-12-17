package com.routerecipt.project.dto;

import lombok.Getter;
import lombok.Setter;

// 서버에 요청하는 데이터 범위를 정하는 객체
@Getter
@Setter
public class Criteria {
    // 현재 페이지
    private int pageNum;
    // 페이지당 보여줄 개수
    private int amount;

    public Criteria() {
        this(1, 10);
    }

    public Criteria(int pageNum, int amount) {
        this.pageNum = pageNum;
        this.amount = amount;
    }

    // 건너 뛸 공지사항 개수
    public int getSkip() {
        return (pageNum - 1) * amount;
    }
}
