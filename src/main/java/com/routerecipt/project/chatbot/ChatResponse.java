package com.routerecipt.project.chatbot;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ChatResponse {
    // 응답 메세지를 담는 클래스: { "reply": "답변 내용" }
    private String reply;
}
