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

    private static final String EMPTY_ASK = "질문 내용을 입력해 주세요.";
    private static final String FALLBACK = "답변을 가져오지 못했어요. 잠시 후 다시 시도해 주세요.";
    private static final String ERROR_MSG = "현재 응답을 생성할 수 없습니다. 잠시 후 다시 시도해 주세요.";

    // ✅ [추가] 문의/버그/오류 질문 고정 답변
    private static final String FORCED_CONTACT =
        "문의 및 버그 제보는 routereceipt@gmail.com으로 접수할 수 있으며 공식 디스코드 서버의 버그 제보 채널로도 신고하실 수 있습니다. 접수 내용은 고객센터와 챗봇을 통해 24시간 전달 가능합니다.";

    // ✅ [추가] 개발자/팀/회사 소개 고정 답변(대표/개발자 수 포함)
    private static final String FORCED_TEAM =
        "routhereceipt는 2025년 10월에 시작된 팀 프로젝트로, 소비자의 과소비를 줄이고 합리적인 소비를 돕기 위해 대표 이장수와 개발자 4명이 함께 만들었습니다.";

    // ✅ [추가] 사용 방법 고정 답변(로그인 필요 + 경로 1회)
    private static final String FORCED_USAGE =
        "로그인이 필요합니다. 메인 화면 → AI 분석하러 가기 → 나의 영수증 분석에서 영수증 이미지를 업로드하면 AI가 항목과 금액, 카테고리와 총액을 분석해 결과를 보여드립니다. 이전 분석 결과도 나의 영수증 분석에서 다시 확인할 수 있으며, 회원가입이 되어 있지 않다면 먼저 회원가입 후 로그인해 주세요.";

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

        // ✅ [추가] 키워드 질문이면 OpenAI 호출 없이 답변 강제
        String forced = forcedReplyIfMatched(userMessage);
        if (forced != null) return new ChatResponse(forced);

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

    // ✅ [추가] 키워드 매칭 후 고정 답변 리턴
    private String forcedReplyIfMatched(String userMessage) {
        String q = (userMessage == null) ? "" : userMessage;

        // 사용 방법/이용 방법/업로드/분석
        if (containsAny(q, "사용 방법", "이용 방법", "사용법", "어떻게 사용", "업로드", "영수증", "AI 분석", "분석", "지출 분석")) {
            return FORCED_USAGE;
        }

        // 문의/고객센터/버그/오류 신고
        if (containsAny(q, "문의", "고객센터", "연락", "버그", "제보", "오류", "에러", "신고")) {
            return FORCED_CONTACT;
        }

        // 개발자/팀/회사 소개
        if (containsAny(q, "개발자", "팀", "팀원", "구성", "회사", "소개", "누가", "만든", "만들")) {
            return FORCED_TEAM;
        }

        return null;
    }

    // ✅ [추가] 키워드 포함 검사
    private boolean containsAny(String text, String... keywords) {
        for (String k : keywords) {
            if (text.contains(k)) return true;
        }
        return false;
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
