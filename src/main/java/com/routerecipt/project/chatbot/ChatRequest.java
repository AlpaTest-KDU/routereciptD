package com.routerecipt.project.chatbot;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChatRequest {
    // 질문 메시지를 담는 요청용 클래스입니다.: { "message": "질문 내용" }
    
    private String message;
}
