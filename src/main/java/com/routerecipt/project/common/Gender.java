package com.routerecipt.project.common;


/**
 * 성별을 표현하는 공통 Enum
 *
 * 목적:
 *  - 시스템 전반에서 성별 값을 일관되게 관리
 *  - 문자열 상수 사용을 방지하여 타입 안정성 확보
 *
 * 구성:
 *  - MALE   : 남성
 *  - FEMALE : 여성
 */
public enum Gender {
	
	/** 남성 */
	MALE("남"),
	
	/** 여성 */
    FEMALE("여");

	
	/**
     * 화면 표시용 라벨
     *  - UI, 응답 JSON, 리포트 출력 등에 사용
     */
    private final String label;

    
    /**
     * Enum 생성자
     *
     * @param label 사용자에게 보여줄 문자열
     */
    Gender(String label) {
        this.label = label;
    }

    
    /**
     * 성별 라벨 반환
     *
     * @return "남" 또는 "여"
     */
    public String getLabel() {
        return label;
    }
}
