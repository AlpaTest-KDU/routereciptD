package com.routerecipt.project.chatbot;

import java.util.Map;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.openai.client.OpenAIClient;
import com.openai.models.ChatModel;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;

@RestController
@RequestMapping("/chatbot")
public class ChatBotController {

   private final OpenAIClient openAIClient;
   private final ChatbotPromptLoader promptLoader;
   
    public ChatBotController(OpenAIClient openAIClient, ChatbotPromptLoader promptLoader) {
        this.openAIClient = openAIClient;
        this.promptLoader = promptLoader;
    }


    @PostMapping("/api/chat")
    public ChatResponse chat(@RequestBody ChatRequest request) {
        String userMessage = (request == null || request.getMessage() == null) ? "" : request.getMessage().trim();
        
        if (userMessage.isEmpty()) {
            return new ChatResponse("질문 내용을 입력해 주세요.");
        }
    
        String systemMessage =  promptLoader.buildSystemMessage();

          ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                .model(ChatModel.GPT_5_1)          // GPT 5.1을 사용해 전문성있게
                .addSystemMessage(systemMessage)   // ✅ 규칙/가이드(스크립트)는 System으로
                .addUserMessage(userMessage)       // ✅ 사용자 질문은 User로
                .temperature(0.2)                  // ✅ 형식/규칙 준수 안정화(권장)
                .build();

        ChatCompletion completion = openAIClient.chat().completions().create(params);


        String reply = (completion.choices() == null || completion.choices().isEmpty())
                ? "답변을 가져오지 못했어요. 잠시 후 다시 시도해 주세요."
                : completion.choices().get(0).message().content()
                    .orElse("답변을 가져오지 못했어요. 잠시 후 다시 시도해 주세요.");
   

        return new ChatResponse(reply);

    
    }
}

