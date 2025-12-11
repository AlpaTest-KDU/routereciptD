package com.routerecipt.project.chatbot;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ChatResponse {
    // 백엔드에서 응답 JSON: { "reply": "답변 내용" }
    private String reply;
}
