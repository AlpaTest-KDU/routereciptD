package com.routerecipt.project.chatbot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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

    private static final Logger log = LoggerFactory.getLogger(ChatBotController.class);

    private static final String EMPTY_ASK  = "질문 내용을 입력해 주세요.";
    private static final String FALLBACK   = "답변을 가져오지 못했어요. 잠시 후 다시 시도해 주세요.";
    private static final String ERROR_MSG  = "현재 응답을 생성할 수 없습니다. 잠시 후 다시 시도해 주세요.";

    // 문의/버그/오류 질문 고정 답변
    private static final String FORCED_CONTACT =
        "문의 및 버그 제보는 routereceipt@gmail.com으로 접수할 수 있으며 공식 디스코드 서버의 버그 제보 채널로도 신고하실 수 있습니다. 접수 내용은 고객센터와 챗봇을 통해 24시간 전달 가능합니다.";

    // 개발자/팀/회사 소개 고정 답변
    private static final String FORCED_TEAM =
        "routereceipt는 2025년 10월에 시작된 팀 프로젝트로, 소비자의 과소비를 줄이고 합리적인 소비를 돕기 위해 대표 이장수와 개발자 4명이 함께 만들었습니다.";

    // 사용 방법 고정 답변
    private static final String FORCED_USAGE =
        "로그인이 필요합니다. ‘지금 시작하기’에서 로그인한 뒤, 메인 화면에서 ‘영수증 분석’을 클릭해 영수증 이미지를 업로드해 주세요. 업로드된 영수증은 시스템이 항목, 금액, 카테고리, 총액을 분석해 결과를 보여드립니다. 또한 마이페이지에서 이전 분석 결과를 다시 확인할 수 있으며, 메인 페이지의 ‘지출 분석’에서 전체 사용자 지출 통계도 확인하실 수 있습니다. 회원가입이 되어 있지 않다면 회원가입 후 로그인해 주세요.";


    private final OpenAIClient openAIClient;
    private final ChatbotPromptLoader promptLoader;

    // ✅ application.properties(openai.model)에서 모델명을 읽음
    @Value("${openai.model:gpt-4.1-mini}")
    private String openaiModelFromProp;

    public ChatBotController(OpenAIClient openAIClient, ChatbotPromptLoader promptLoader) {
        this.openAIClient = openAIClient;
        this.promptLoader = promptLoader;
    }

    @PostMapping("/chat")
    public ChatResponse chat(@RequestBody ChatRequest request) {

        // 1) 입력 안전 처리 + 1줄 정규화
        String userMessage = (request == null || request.getMessage() == null) ? "" : request.getMessage();
        userMessage = oneLine(userMessage);

        // ✅ 배포 반영/인스턴스 확인용 (임시)
        // 배포 후 챗봇에 MARKER 입력 시 아래 문자열이 나오면 "이 코드가 실제로 응답 중" 확정
        if ("MARKER".equals(userMessage)) {
            return new ChatResponse("MARK-2025-12-23-A");
        }

        if (userMessage.isEmpty()) {
            return new ChatResponse(EMPTY_ASK);
        }

        // 2) 강제 답변 분기 (OpenAI 호출 전에 처리) - 요구사항대로 광범위 매칭
        String forced = forcedReplyIfMatched(userMessage);
        log.info("chat request='{}' forced={}", userMessage, (forced != null));
        if (forced != null) {
            // ✅ 강제답변도 공백 방지 (원칙상 공백일 일은 없지만 안전장치)
            String out = forced == null ? "" : forced.trim();
            return new ChatResponse(out.isEmpty() ? FALLBACK : out);
        }

        // 3) 시스템 메시지 로딩
        String systemMessage = promptLoader.buildSystemMessage();

        // 4) OpenAI 호출
        try {
            ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                .model(resolveModelFromProperty())  // ✅ properties 기반 모델
                .addSystemMessage(systemMessage)
                .addUserMessage(userMessage)
                .temperature(0.2)
                .maxCompletionTokens(260)
                .build();

            ChatCompletion completion = openAIClient.chat().completions().create(params);

            if (completion == null || completion.choices() == null || completion.choices().isEmpty()) {
                return new ChatResponse(FALLBACK);
            }

            String raw = completion.choices().get(0).message().content().orElse("");

            // ✅ 핵심: raw가 빈/공백이면 절대 그대로 반환하지 않음
            if (raw == null || raw.trim().isEmpty()) {
                return new ChatResponse(FALLBACK);
            }

            String reply = sanitizeReply(raw);

            // ✅ 핵심: sanitize 이후도 빈/공백이면 FALLBACK
            String out = (reply == null) ? "" : reply.trim();
            return new ChatResponse(out.isEmpty() ? FALLBACK : out);

        } catch (Exception e) {
            log.warn("OpenAI call failed", e);
            return new ChatResponse(ERROR_MSG);
        }
    }

    // ---------------------------
    // Forced reply matcher
    // ---------------------------
    private String forcedReplyIfMatched(String userMessage) {
        String norm = normalizeForMatch(userMessage);
        String compact = norm.replace(" ", "");

        // 사용 방법/이용 방법/업로드/분석 -> 요구사항대로 넓게 매칭
        if (containsAny(norm, "사용 방법", "이용 방법", "사용법", "어떻게 사용", "업로드", "영수증", "ai 분석", "분석", "지출 분석")
            || containsAny(compact, "사용방법", "이용방법", "사용법", "어떻게사용", "영수증업로드", "ai분석", "지출분석")) {
            return FORCED_USAGE;
        }

        // 문의/고객센터/버그/오류 신고
        if (containsAny(norm, "문의", "고객센터", "연락", "버그", "제보", "오류", "에러", "신고")) {
            return FORCED_CONTACT;
        }

        // 개발자/팀/회사 소개
        if (containsAny(norm, "개발자", "팀", "팀원", "구성", "회사", "소개", "누가", "만든", "만들")) {
            return FORCED_TEAM;
        }

        return null;
    }

    private boolean containsAny(String text, String... keywords) {
        if (text == null) return false;
        for (String k : keywords) {
            if (k != null && !k.isEmpty() && text.contains(k)) return true;
        }
        return false;
    }

    private String normalizeForMatch(String s) {
        if (s == null) return "";
        // NBSP 등 유니코드 공백을 일반 공백으로
        String x = s.replace('\u00A0', ' ');
        x = x.toLowerCase();
        x = x.replaceAll("[\\r\\n]+", " ");
        x = x.replaceAll("\\s{2,}", " ").trim();
        return x;
    }

    // ---------------------------
    // Model resolver (properties 기반 / enum 의존 제거)
    // ---------------------------
    private ChatModel resolveModelFromProperty() {
        String m = (openaiModelFromProp == null) ? "" : openaiModelFromProp.trim();
        if (m.isEmpty()) m = "gpt-4.1-mini";

        try {
            return ChatModel.of(m);
        } catch (Throwable t) {
            log.warn("Invalid/unsupported openai.model='{}' -> fallback to gpt-4.1-mini", m, t);
            return ChatModel.of("gpt-4.1-mini");
        }
    }

    // ---------------------------
    // Input/Output normalization
    // ---------------------------
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

        // 2) 목록/제목 흔적 최소 제거
        x = x.replace("•", " ");
        x = x.replaceAll("\\s-\\s", " "); // " - " 형태 제거
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
        return Math.max(Math.max(p1 + 2, p2 + 3), Math.max(p3 + 2, p4 + 1));
    }
}
