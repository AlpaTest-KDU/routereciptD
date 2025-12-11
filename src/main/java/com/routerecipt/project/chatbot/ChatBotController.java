package com.routerecipt.project.chatbot;

import org.springframework.web.bind.annotation.*;

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
    @PostMapping("/ask")
    public String askPost(@RequestParam("question") String question) {
        return chatBotService.ask(question);
    }
}
