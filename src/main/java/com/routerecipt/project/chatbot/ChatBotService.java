package com.routerecipt.project.chatbot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.openai.client.OpenAIClient;
import com.openai.models.ChatModel;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;


/**
 * ChatBotService
 *
 * 역할:
 *  1) 사용자 메시지 입력값 정리/검증
 *  2) 특정 질문은 OpenAI 호출 없이 고정 문구로 즉시 응답(Forced Reply)
 *  3) System Prompt(파일 기반)를 로드하여 OpenAI에 전달
 *  4) 모델 응답을 후처리(sanitize)하여 UI에 적합하게 반환
 */
@Service
public class ChatBotService {

    private static final Logger log = LoggerFactory.getLogger(ChatBotService.class);
    
    /** 빈 질문일 때 반환할 안내 메시지 */
    private static final String EMPTY_ASK  = "질문 내용을 입력해 주세요.";
    
    /** OpenAI 응답이 비정상/비어있을 때 반환할 기본 메시지 */
    private static final String FALLBACK   = "답변을 가져오지 못했어요. 잠시 후 다시 시도해 주세요.";
    
    /** 예외 발생 시 반환할 메시지 */
    private static final String ERROR_MSG  = "현재 응답을 생성할 수 없습니다. 잠시 후 다시 시도해 주세요.";

    // ---------------- 강제(고정) 답변 텍스트 ----------------
    
    /** 문의/버그/오류 관련 질문에 대한 고정 안내 */
    private static final String FORCED_CONTACT =
        "문의 및 버그 제보는 routereceipt@gmail.com으로 접수할 수 있으며 공식 디스코드 서버의 버그 제보 채널로도 신고하실 수 있습니다. 접수 내용은 고객센터와 챗봇을 통해 24시간 전달 가능합니다.";

    /** 팀/개발자 소개 관련 질문에 대한 고정 안내 */
    private static final String FORCED_TEAM =
        "routereceipt는 2025년 10월에 시작된 팀 프로젝트로, 소비자의 과소비를 줄이고 합리적인 소비를 돕기 위해 대표 이장수와 개발자 4명이 함께 만들었습니다.";

    /** 사용 방법 관련 질문에 대한 고정 안내 */
    private static final String FORCED_USAGE =
          "로그인이 필요합니다. ‘지금 시작하기’에서 로그인한 뒤, 메인 화면에서 ‘영수증 분석’을 클릭해 영수증 이미지를 업로드해 주세요. 업로드된 영수증은 시스템이 항목, 금액, 카테고리, 총액을 분석해 결과를 보여드립니다. 또한 마이페이지에서 이전 분석 결과를 다시 확인할 수 있으며, 메인 페이지의 ‘지출 분석’에서 전체 사용자 지출 통계도 확인하실 수 있습니다. 회원가입이 되어 있지 않다면 회원가입 후 로그인해 주세요.";

    // ---------------- 의존성 ----------------
    
    /** OpenAI 호출 클라이언트 (Config에서 Bean으로 등록됨) */
    private final OpenAIClient openAIClient;
    
    /** System Prompt(템플릿 + 서비스 info) 로더 */
    private final ChatbotPromptLoader promptLoader;

    
    /**
     * 사용할 모델 이름 (설정값 우선, 없으면 gpt-4.1-mini)
     * 예: openai.model=gpt-4.1-mini
     */
    @Value("${openai.model:gpt-4.1-mini}")
    private String openaiModelFromProp;

    public ChatBotService(OpenAIClient openAIClient, ChatbotPromptLoader promptLoader) {
        this.openAIClient = openAIClient;
        this.promptLoader = promptLoader;
    }

    
    /**
     * 사용자 메시지를 받아 챗봇 응답 생성
     *
     * @param rawMessage 사용자 입력 (null 가능)
     * @return 챗봇 응답 문자열
     */
    public String generateChatResponse(String rawMessage) {
        // 1) 입력 안전 처리 + 1줄 정규화
        String userMessage = (rawMessage == null) ? "" : rawMessage;
        userMessage = oneLine(userMessage);

        // ✅ 배포 반영/인스턴스 확인용 (임시)
        if ("MARKER".equals(userMessage)) {
            return "MARK-2025-12-23-A";
        }
        
        // 1-1) 빈 입력 방어
        if (userMessage.isEmpty()) {
            return EMPTY_ASK;
        }

        // 2) 강제 답변 분기 (안내/문의/팀소개 등은 OpenAI 호출 없이 즉시 응답)
        String forced = forcedReplyIfMatched(userMessage);
        log.info("chat request='{}' forced={}", userMessage, (forced != null));
        if (forced != null) {
            String out = forced.trim();
            return out.isEmpty() ? FALLBACK : out;
        }

        // 3) System 메시지 로딩 (기동 시 캐싱된 값)
        String systemMessage = promptLoader.buildSystemMessage();

        // 4) OpenAI 호출
        try {
            ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                .model(resolveModelFromProperty())		// 설정된 모델명 파싱 + fallback
                .addSystemMessage(systemMessage)		// 시스템 프롬프트
                .addUserMessage(userMessage)			// 사용자 질문
                .temperature(0.2)						// 답변 흔들림 최소화
                .maxCompletionTokens(260)				// 답변 길이 제한
                .build();

            ChatCompletion completion = openAIClient.chat().completions().create(params);
            
            // 4-1) 응답 구조 검증
            if (completion == null || completion.choices() == null || completion.choices().isEmpty()) {
                return FALLBACK;
            }

            // 4-2) 첫 번째 choice의 content 추출
            String raw = completion.choices().get(0).message().content().orElse("");

            if (raw == null || raw.trim().isEmpty()) {
                return FALLBACK;
            }

            // 4-3) 응답 후처리 (개행/기호/길이 제한)
            String reply = sanitizeReply(raw);
            String out = (reply == null) ? "" : reply.trim();
            return out.isEmpty() ? FALLBACK : out;

        } catch (Exception e) {
        	// 네트워크/모델/SDK 예외 등 모든 실패를 안전한 메시지로 감싼다
            log.warn("OpenAI call failed", e);
            return ERROR_MSG;
        }
    }
    
    /**
     * 특정 유형(사용법/문의/소개)의 질문에 대해 고정 답변을 반환
     * @return 매칭되면 고정 답변 문자열, 아니면 null
     */
    private String forcedReplyIfMatched(String userMessage) {
        String norm = normalizeForMatch(userMessage);
        String compact = norm.replace(" ", "");
        
        // 사용 방법 관련
        if (containsAny(norm, "사용 방법", "이용 방법", "사용법", "어떻게 사용", "업로드", "영수증", "ai 분석", "분석", "지출 분석")
            || containsAny(compact, "사용방법", "이용방법", "사용법", "어떻게사용", "영수증업로드", "ai분석", "지출분석")) {
            return FORCED_USAGE;
        }
        
        // 문의/버그/오류 관련  
        if (containsAny(norm, "문의", "고객센터", "연락", "버그", "제보", "오류", "에러", "신고")) {
            return FORCED_CONTACT;
        }
        
        // 팀/개발자/소개 관련
        if (containsAny(norm, "개발자", "팀", "팀원", "구성", "회사", "소개", "누가", "만든", "만들")) {
            return FORCED_TEAM;
        }

        return null;
    }
    
    /** text에 keywords 중 하나라도 포함되면 true */
    private boolean containsAny(String text, String... keywords) {
        if (text == null) return false;
        for (String k : keywords) {
            if (k != null && !k.isEmpty() && text.contains(k)) return true;
        }
        return false;
    }
    
    /**
     * 강제답변 매칭용 정규화:
     *  - NBSP 제거
     *  - 소문자화
     *  - 개행 제거
     *  - 연속 공백 정리
     */
    private String normalizeForMatch(String s) {
        if (s == null) return "";
        String x = s.replace('\u00A0', ' ');
        x = x.toLowerCase();
        x = x.replaceAll("[\\r\\n]+", " ");
        x = x.replaceAll("\\s{2,}", " ").trim();
        return x;
    }
    
    
    /**
     * 설정된 openai.model 값을 ChatModel로 변환하고,
     * 잘못된 값이면 gpt-4.1-mini로 안전 fallback
     */
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
    
    /** 사용자 입력을 1줄로 정리(개행 제거 + 공백 정리) */
    private String oneLine(String s) {
        if (s == null) return "";
        String x = s.replaceAll("[\\r\\n]+", " ");
        x = x.replaceAll("\\s{2,}", " ").trim();
        return x;
    }
    
    /**
     * 모델 응답 후처리:
     *  - 개행 제거
     *  - 불필요한 기호 일부 제거
     *  - 최대 길이 제한(450자) + 문장 끝에서 자르기
     */
    private String sanitizeReply(String s) {
        if (s == null) return "";
        String x = s;
        x = x.replaceAll("[\\r\\n]+", " ");
        x = x.replaceAll("\\s{2,}", " ").trim();
        x = x.replace("•", " ");
        x = x.replaceAll("\\s-\\s", " ");
        x = x.replace("#", " ");
        x = x.replaceAll("\\s{2,}", " ").trim();

        if (x.length() > 450) {
            String cut = x.substring(0, 450);
            int end = lastSentenceEnd(cut);
            if (end >= 40) cut = cut.substring(0, end).trim();
            x = cut.trim();
        }
        return x;
    }
    
    
    /**
     * 문장 끝 위치 추정:
     *  - "요.", "니다.", "다." 같은 한국어 종결 형태 또는 . ! ? 기준
     * @return 마지막 종결 위치(없으면 -1)
     */
    private int lastSentenceEnd(String s) {
        if (s == null || s.isEmpty()) return -1;
        int p1 = s.lastIndexOf("요.");
        int p2 = s.lastIndexOf("니다.");
        int p3 = s.lastIndexOf("다.");
        int p4 = Math.max(s.lastIndexOf("."), Math.max(s.lastIndexOf("!"), s.lastIndexOf("?")));
        
        // lastIndexOf는 찾지 못하면 -1이므로, +n 시 값이 작아질 수 있음.
        // 하지만 최종 Math.max에서 자연스럽게 걸러짐.
        return Math.max(Math.max(p1 + 2, p2 + 3), Math.max(p3 + 2, p4 + 1));
    }
}