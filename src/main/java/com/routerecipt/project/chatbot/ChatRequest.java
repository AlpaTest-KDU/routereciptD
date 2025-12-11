package com.routerecipt.project.chatbot;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChatRequest {
    // 프론트에서 보내는 JSON: { "message": "질문 내용" }
    private String message;
}
