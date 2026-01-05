package com.routerecipt.project.dto;

public enum ItemCategory {
	FOOD("식비"),
    CLOTHES("의류"),
    MEDICAL("의료"),
    TRAFFIC("교통"),
    CULTURE("문화"),
    HOME("주거"),
    LIVING("생활"),
    ETC("기타");

    // 마이페이지 등에서 나타낼 때 영어로 나오는 부분을 한국어로 나오게 하기 위해 추가
    private final String koreanName;

    ItemCategory(String koreanName) {
        this.koreanName = koreanName;
    }

    public String getKoreanName() {
        return koreanName;
    }
}
