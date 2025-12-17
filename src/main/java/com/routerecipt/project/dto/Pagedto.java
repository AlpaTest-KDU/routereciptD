package com.routerecipt.project.dto;

import lombok.Getter;

@Getter
public class Pagedto {
    private int startPage;
    private int endPage;
    private boolean prev;
    private boolean next;
    private int total;
    private Criteria criteria;

    public Pagedto(Criteria criteria, int total) {
        this.criteria = criteria;
        this.total = total;

        // 끝 페이지 계산
        // 한 페이지당 10개씩
        this.endPage = (int) (Math.ceil(criteria.getPageNum() / 10.0)) * 10;
        this.startPage = this.endPage - 9;

        int realEndPage = (int) (Math.ceil((total * 1.0) / criteria.getAmount()));
        if (realEndPage < this.endPage) {
            this.endPage = realEndPage;
        }

        this.prev = this.startPage > 1;
        this.next = this.endPage < realEndPage;
    }
}
