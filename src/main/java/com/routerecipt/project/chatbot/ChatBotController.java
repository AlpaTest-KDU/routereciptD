package com.routerecipt.project.chatbot;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * ChatBot API Controller
 *
 * 역할:
 *  - 클라이언트로부터 사용자 메시지 수신
 *  - ChatBotService 호출
 *  - AI 응답을 JSON 형태로 반환
 *
 * URL 구조:
 *  POST /api/chat
 */
@RestController
@RequestMapping("/api")
public class ChatBotController {
	
	/** 챗봇 비즈니스 로직 처리 서비스 */
    private final ChatBotService chatBotService;
    
    /**
     * 생성자 주입 (권장 방식)
     *
     * @param chatBotService 챗봇 서비스
     */
    public ChatBotController(ChatBotService chatBotService) {
        this.chatBotService = chatBotService;
    }
    
    /**
     * 챗봇 메시지 처리 API
     *
     * @param request 사용자 메시지를 담은 요청 DTO
     * @return AI 응답을 담은 응답 DTO
     */
    @PostMapping("/chat")
    public ChatResponse chat(@RequestBody ChatRequest request) {
    	// 요청이 null일 경우를 대비한 방어 코드
        String message = (request == null) ? null : request.getMessage();
        
        // 챗봇 서비스 호출 → AI 응답 생성
        String response = chatBotService.generateChatResponse(message);
        
        // 응답 DTO로 감싸서 반환
        return new ChatResponse(response);
    }
}
