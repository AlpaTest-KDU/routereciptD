package com.routerecipt.project.chatbot;

import org.springframework.web.bind.annotation.*;

import com.openai.client.OpenAIClient;
import com.openai.models.ChatModel;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;

@RestController
@RequestMapping("/api")
public class ChatBotController {

    private static final String EMPTY_ASK = "질문 내용을 입력해 주세요.";
    private static final String FALLBACK = "답변을 가져오지 못했어요. 잠시 후 다시 시도해 주세요.";
    private static final String ERROR_MSG = "현재 응답을 생성할 수 없습니다. 잠시 후 다시 시도해 주세요.";

    private final OpenAIClient openAIClient;
    private final ChatbotPromptLoader promptLoader;

    public ChatBotController(OpenAIClient openAIClient, ChatbotPromptLoader promptLoader) {
        this.openAIClient = openAIClient;
        this.promptLoader = promptLoader;
    }

    @PostMapping("/chat")
    public ChatResponse chat(@RequestBody ChatRequest request) {
        String userMessage = (request == null || request.getMessage() == null) ? "" : request.getMessage().trim();
        userMessage = oneLine(userMessage);
        if (userMessage.isEmpty()) return new ChatResponse(EMPTY_ASK);

        String systemMessage = promptLoader.buildSystemMessage();

        try {
            ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                .model(resolveStableModel())              // ✅ 안정형 모델
                .addSystemMessage(systemMessage)
                .addUserMessage(userMessage)
                .temperature(0.2)
                .maxCompletionTokens(260)                 // 약간 여유
                .build();

            ChatCompletion completion = openAIClient.chat().completions().create(params);

            if (completion.choices() == null || completion.choices().isEmpty()) {
                return new ChatResponse(FALLBACK);
            }

            String raw = completion.choices().get(0).message().content().orElse("");
            String reply = sanitizeReply(raw);

            return new ChatResponse(reply.isEmpty() ? FALLBACK : reply);

        } catch (Exception e) {
            return new ChatResponse(ERROR_MSG);
        }
    }

    // SDK 버전에 따라 문자열 모델 지정 지원 여부가 달라서 안전하게 처리
    private ChatModel resolveStableModel() {
        try {
            return ChatModel.of("gpt-5.2");   // ✅ 안정형 고정 모델
        } catch (Throwable t) {
            return ChatModel.GPT_5_1;        // 폴백
        }
    }

    private String oneLine(String s) {
        if (s == null) return "";
        String x = s.replaceAll("[\\r\\n]+", " ");
        x = x.replaceAll("\\s{2,}", " ").trim();
        return x;
    }

    // 프롬프트가 규칙을 잘 지키더라도, 최종 단계에서 서버가 100% 보정
    private String sanitizeReply(String s) {
        if (s == null) return "";
        String x = s;

        // 1) 한 단락 강제: 줄바꿈 제거
        x = x.replaceAll("[\\r\\n]+", " ");
        x = x.replaceAll("\\s{2,}", " ").trim();

        // 2) 목록/제목 흔적 최소 제거(규칙 위반 방지)
        //    "-" 자체를 전부 지우면 문장 의미가 깨질 수 있어, " - " 패턴만 정리
        x = x.replace("•", " ");
        x = x.replaceAll("\\s-\\s", " ");   // " - " 형태 제거
        x = x.replace("#", " ");
        x = x.replaceAll("\\s{2,}", " ").trim();

        // 3) 450자 제한(초과 시 문장 끝 기준으로 자르기 시도)
        if (x.length() > 450) {
            String cut = x.substring(0, 450);
            int end = lastSentenceEnd(cut);
            if (end >= 40) cut = cut.substring(0, end).trim();
            x = cut.trim();
        }

        return x;
    }

    private int lastSentenceEnd(String s) {
        if (s == null || s.isEmpty()) return -1;
        int p1 = s.lastIndexOf("요.");
        int p2 = s.lastIndexOf("니다.");
        int p3 = s.lastIndexOf("다.");
        int p4 = Math.max(s.lastIndexOf("."), Math.max(s.lastIndexOf("!"), s.lastIndexOf("?")));
        int end = Math.max(Math.max(p1 + 2, p2 + 3), Math.max(p3 + 2, p4 + 1));
        return end;
    }
}
