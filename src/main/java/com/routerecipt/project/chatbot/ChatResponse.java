package com.routerecipt.project.chatbot;

import lombok.AllArgsConstructor;
import lombok.Getter;


/**
 * 챗봇 응답(Response) DTO
 *
 * 용도:
 *  - /api/chat API의 응답 Body 매핑
 *
 * 응답 예시:
 * {
 *   "reply": "답변 내용"
 * }
 */
@Getter
@AllArgsConstructor
public class ChatResponse {
	/**
     * 챗봇이 생성한 최종 응답 메시지
     * - 이미 후처리(sanitize)된 문자열
     * - 프론트에서는 그대로 출력하면 됨
     */
    private String reply;
}