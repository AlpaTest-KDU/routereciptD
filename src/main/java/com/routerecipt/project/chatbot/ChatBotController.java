package com.routerecipt.project.chatbot;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.openai.client.OpenAIClient;
import com.openai.models.ChatModel;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;

@RestController
@RequestMapping("/api")
public class ChatBotController {

   private final OpenAIClient openAIClient;
   private final ChatbotPromptLoader promptLoader;
   
    public ChatBotController(OpenAIClient openAIClient, ChatbotPromptLoader promptLoader) {
        this.openAIClient = openAIClient;
        this.promptLoader = promptLoader;
    }


 
    @PostMapping("/chat")
    public ChatResponse chat(@RequestBody ChatRequest request) {
        String userMessage = (request == null || request.getMessage() == null) ? "" : request.getMessage().trim();
        if (userMessage.isEmpty()) return new ChatResponse("질문 내용을 입력해 주세요.");

        String systemMessage = promptLoader.buildSystemMessage();

        try {
            ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                .model(ChatModel.GPT_5_1)
                .addSystemMessage(systemMessage)
                .addUserMessage(userMessage)
                .temperature(0.2)
                .maxCompletionTokens(220)
                .build();

            ChatCompletion completion = openAIClient.chat().completions().create(params);

            String reply = "답변을 가져오지 못했어요. 잠시 후 다시 시도해 주세요.";
            if (completion.choices() != null && !completion.choices().isEmpty()) {
                reply = completion.choices().get(0).message().content().orElse("").trim();
                if (reply.isEmpty()) reply = "답변을 가져오지 못했어요. 잠시 후 다시 시도해 주세요.";
            }
            return new ChatResponse(reply);
        } catch (Exception e) {
            return new ChatResponse("현재 응답을 생성할 수 없습니다. 잠시 후 다시 시도해 주세요.");
        }
    }

}

