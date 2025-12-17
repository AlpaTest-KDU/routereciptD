package com.routerecipt.project.chatbot;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.openai.client.OpenAIClient;
import com.openai.models.ChatModel;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;

/**
 * ✅ ChatBotController 역할
 * - 프론트(웹 화면)에서 사용자가 채팅을 입력하면
 * - 서버가 그 질문을 OpenAI에 보내서
 * - OpenAI가 만든 답변을 다시 프론트로 JSON 형태로 돌려주는 컨트롤러
 */
@RestController
@RequestMapping("/chatbot")
public class ChatBotController {

    /**
     * ✅ OpenAI API를 호출하는 객체(클라이언트)
     * - ChatBotConfig에서 @Bean으로 만들어둔 OpenAIClient가 여기로 주입됨
     * - 한 번 생성된 클라이언트를 재사용하는 구조
     */
    private final OpenAIClient openAIClient;

    /**
     * ✅ ROUTERECEIPT_INFO 역할
     * - "우리 서비스(routereceipt) 안내 챗봇"처럼 답변하게 만드는 설명서
     * - 서비스 사용 방법 / 문의 방법 / 회사 소개 / 답변 규칙 등이 들어있음
     * - 이 텍스트를 매번 OpenAI에게 같이 보내서
     *   사용자가 어떤 질문을 해도 routereceipt 안내 범위 안에서 답하도록 유도함
     */
    private static final String ROUTERECEIPT_INFO = """
            [routereceipt 서비스 사용방법]
                    
                    """;

    public ChatBotController(OpenAIClient openAIClient) {
        this.openAIClient = openAIClient;
    }

    /**
     * ✅ 프론트에서 이 API를 호출하는 예시
     * - POST /api/chat
     * - Body(JSON): { "message": "질문 내용" }
     *
     * ✅ 서버가 하는 일
     * 1) 사용자의 질문을 받음
     * 2) 서비스 설명(ROUTERECEIPT_INFO) + 답변 규칙을 "System" 메시지로 설정함 (규칙을 더 잘 지키게 만드는 핵심)
     * 3) 사용자 질문을 "User" 메시지로 전달함
     * 4) OpenAI에 요청해서 답변을 생성함
     * 5) 생성된 답변을 ChatResponse(reply)에 담아 JSON으로 반환함
     */
    @PostMapping("/api/chat")
    public ChatResponse chat(@RequestBody ChatRequest request) {

        /* 사용자 질문 */
        String userMessage = request.getMessage();

        // ✅ (핵심) 규칙/서비스 안내는 System 메시지로 올려야 "규칙을 무시"하는 현상이 크게 줄어듭니다.
        // - 기존처럼 모든 내용을 addUserMessage(prompt)로 보내면,
        //   모델이 규칙을 "사용자 텍스트"처럼 취급할 수 있어 준수도가 흔들립니다.
        String systemMessage = """
        당신은 'routereceipt' 팀이 만든 공식 안내 챗봇입니다.

        [서비스 설명]
        %s

        [질문/응답 규칙]
        - routereceipt 서비스, 영수증 업로드, 지출 분석, 캘린더, 영수증 관리, 게시판, 회사 소개와 직접 관련된 질문만 답변합니다.
        - 위 주제와 무관한 일반 상식이나 다른 서비스(예: 토스, 은행 앱 등)에 대한 질문이 들어오면
          "이 챗봇은 routereceipt 서비스 관련 질문만 도와드립니다."라고 먼저 안내해 주세요.
        - 사용자가 '어디서' '무엇을' '어떻게' 하는지 물으면, 페이지 이름, 메뉴 위치, 버튼 이름, 이동 경로를 중심으로 단계별로 설명해 주세요.
        - 회원가입이나 로그인이 필요한 기능이라면, 먼저 회원가입/로그인 필요 여부를 함께 안내해 주세요.
        - 답변은 한국어로 3~6문장 사이로만 작성하세요.
        - 별표(*)나 마크다운(**텍스트**)은 사용하지 말고, 일반 문장으로만 답변하세요.
        """.formatted(ROUTERECEIPT_INFO);

        // 3) OpenAI API 요청 파라미터 만들기
        //    - 어떤 모델을 사용할지 지정
        //    - System(규칙/지침) + User(질문) 형태로 분리해서 전달
        ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                .model(ChatModel.GPT_5_1)          // GPT 5.1을 사용해 전문성있게
                .addSystemMessage(systemMessage)   // ✅ 규칙/가이드(스크립트)는 System으로
                .addUserMessage(userMessage)       // ✅ 사용자 질문은 User로
                .temperature(0.2)                  // ✅ 형식/규칙 준수 안정화(권장)
                .build();

        ChatCompletion completion = openAIClient.chat()
                .completions()
                .create(params);

        // 5) OpenAI 응답에서 답변 문자열(reply)만 꺼내기
        //    - choices(여러 후보 답변) 중 첫 번째(0번)만 사용
        //    - content가 비어있으면 기본 문구로 대체
        String reply;
        if (completion.choices() == null || completion.choices().isEmpty()) {
            reply = "답변을 가져오지 못했어요. 잠시 후 다시 시도해 주세요.";
        } else {
            reply = completion.choices()
                    .get(0)
                    .message()
                    .content()
                    .orElse("답변을 가져오지 못했어요. 잠시 후 다시 시도해 주세요.");
        }

        return new ChatResponse(reply);
    }
}
