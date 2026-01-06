package com.routerecipt.project.chatbot;

import lombok.Getter;
import lombok.Setter;



/**
 * 챗봇 요청(Request) DTO
 *
 * 용도:
 *  - /api/chat API로 전달되는 JSON 요청 Body 매핑
 *
 * 요청 예시:
 * {
 *   "message": "질문 내용"
 * }
 */
@Getter
@Setter
public class ChatRequest {
	/**
     * 사용자가 입력한 질문 메시지
     * - null 또는 빈 문자열일 수 있으므로
     *   실제 검증/처리는 Service 레벨에서 수행
     */
    private String message;
}
