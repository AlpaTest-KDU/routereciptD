package com.routerecipt.project.dto;


/**
 * 영수증 아이템 카테고리 Enum
 *
 * 역할:
 *  - 영수증에 포함된 각 상품(Item)의 카테고리 정의
 *  - 시스템 내부 코드값과 화면 표시용 한글명을 분리하여 관리
 */
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

    
    /**
     * Enum 생성자
     *
     * @param koreanName 사용자에게 보여줄 한글 이름
     */
    ItemCategory(String koreanName) {
        this.koreanName = koreanName;
    }

    
    /**
     * 한글 카테고리 이름 반환
     *
     * @return 한글 카테고리명 (예: "식비")
     */
    public String getKoreanName() {
        return koreanName;
    }
}
