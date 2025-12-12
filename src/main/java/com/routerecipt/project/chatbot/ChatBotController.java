package com.routerecipt.project.chatbot;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/chatbot")
public class ChatBotController {

    private final ChatBotService chatBotService;

    public ChatBotController(ChatBotService chatBotService) {
        this.chatBotService = chatBotService;
    }

    // GET 테스트
    @GetMapping("/ask")
    public String askGet(@RequestParam("question") String question) {
        return chatBotService.ask(question);
    }

    // POST
    // CSRF 토큰으로 인해서 JSON 데이터를 받아와야 해서 RequestBody로 변경
    @PostMapping("/ask")
    public Map<String, String> askPost(@RequestBody ChatRequest request) {
        String answer = chatBotService.ask(request.getMessage());
        return Map.of("reply", answer);
    }
}
