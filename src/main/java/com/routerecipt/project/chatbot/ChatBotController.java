package com.routerecipt.project.chatbot;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.openai.client.OpenAIClient;
import com.openai.models.ChatModel;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;

@RestController
public class ChatBotController {
	
	private final OpenAIClient openAIClient;

    // routereceipt 서비스 / 회사 / 사용방법 안내 텍스트
    // routereceipt 서비스/회사/사용 방법 안내용 시스템 프롬프트
private static final String ROUTERECEIPT_INFO = """
            [routereceipt 서비스 사용방법]

            1) 아이디가 있을 때
            - 메인 화면에서 "영수증 등록하기" 버튼을 누릅니다.
            - 영수증 이미지를 선택해 업로드하고, routereceipt가 분석을 마칠 때까지 기다립니다.
            - 분석이 끝난 후 "나의 영수증 분석" 버튼을 눌러 결과를 확인할 수 있습니다.
            - 분석된 데이터를 다시 보고 싶다면 메인 화면에서 "지출 분석" 메뉴로 이동합니다.

            2) 아이디가 없을 때
            - 먼저 회원 가입을 완료하신 후 서비스 이용이 가능합니다.

            [문의 방법]
            - 문의 사항은 "고객센터" 페이지를 통해 챗봇으로 문의하실 수 있습니다.
            - routereceipt 고객센터는 24시간 문의를 받고 있습니다.

            [버그 제보]
            - 버그 제보는 routereceipt@gmail.com으로 보내주시면 감사히 확인하겠습니다. 또한 공식 디스코드 서버의 버그 제보 채널에서도 문의가 가능합니다.
            - 중요한 버그 제보의 경우 소정의 상품이 제공될 예정이니 많은 관심 부탁드립니다.

            [공지 사항 확인 방법]
            - 공지 사항은 메인 화면에서 "공지 사항" 버튼을 눌러 확인할 수 있습니다.

            [routereceipt 서비스 개요]
            - 영수증 사진 한 장만 있으면, 최첨단 기술을 탑재한 routereceipt AI가 자동으로 항목과 금액을 인식하고 지출 리포트를 만들어 드립니다.
            - 영수증 업로드:
              사용자가 갤러리에 저장된 영수증 이미지를 선택해 업로드합니다.
            - AI가 항목·금액 인식:
              routereceipt AI 기반 모델이 항목, 금액, 카테고리, 총액 등을 자동으로 추출합니다.
            - 지출 분석 확인:
              캘린더 형식으로 해당 달의 지출을 한눈에 볼 수 있고, 일/주/월별 그래프와 통계를 통해 나의 소비 습관을 한눈에 확인할 수 있습니다.

            [routereceipt 회사 소개]
            - routereceipt는 2025년 10월에 설립된 회사입니다.
            - 소비자들의 과소비를 줄이기 위해 대표 이장수와 각 분야의 개발자 4명이 함께 만든 서비스입니다.

            [답변 규칙]
            - 위 내용을 기반으로 routereceipt 서비스, 회사, 게시판, 문의, 사용 방법에 대해 안내해 주세요.
            - 버튼 이름과 이동 경로(예: "메인 화면 -> 영수증 등록하기 -> 나의 영수증 분석")를 구체적으로 알려주세요.
            - 별표(*) 문자나 마크다운 형식(**굵게**, _기울임_ 등)은 사용하지 마세요.
            - 일반 문장으로만 답변하세요.
            - 모르는 내용이나 아직 구현되지 않은 기능은 "추후 구현 예정" 또는 "현재는 지원하지 않습니다"라고 정직하게 답변하세요.
            - 답변은 한국어로 3~6문장 정도로 친절하게 작성하세요.
            """;

    public ChatBotController(OpenAIClient openAIClient) {
        this.openAIClient = openAIClient;
    }


    @PostMapping("/api/chat")
    public ChatResponse chat(@RequestBody ChatRequest request) {

        String userMessage = request.getMessage();

        // 사용자 질문 + routereceipt 설명을 합친 프롬프트
        String prompt = """
        당신은 'routereceipt' 팀이 만든 공식 안내 챗봇입니다.

        [서비스 설명]
        %s

        [사용자 질문]
        %s

        [질문/응답 규칙]
        - routereceipt 서비스, 영수증 업로드, 지출 분석, 캘린더, 영수증 관리, 게시판, 회사 소개와 직접 관련된 질문만 답변합니다.
        - 위 주제와 무관한 일반 상식이나 다른 서비스(예: 토스, 은행 앱 등)에 대한 질문이 들어오면
        "이 챗봇은 routereceipt 서비스 관련 질문만 도와드립니다."라고 먼저 안내해 주세요.
        - 사용자가 '어디서' '무엇을' '어떻게' 하는지 물으면, 페이지 이름, 메뉴 위치, 버튼 이름, 이동 경로를 중심으로 단계별로 설명해 주세요.
        - 회원가입이나 로그인이 필요한 기능이라면, 먼저 회원가입/로그인 필요 여부를 함께 안내해 주세요.
        - 답변은 한국어로 3~6문장 사이로만 작성하세요.
        - 별표(*)나 마크다운(**텍스트**)은 사용하지 말고, 일반 문장으로만 답변하세요.
        """.formatted(ROUTERECEIPT_INFO, userMessage);



        ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                .model(ChatModel.GPT_5_1)
                .addUserMessage(prompt)
                .build();

        ChatCompletion completion = openAIClient.chat()
                .completions()
                .create(params);

        String reply = completion.choices()
                .get(0)
                .message()
                .content()
                .orElse("답변을 가져오지 못했어요. 잠시 후 다시 시도해 주세요.");

        return new ChatResponse(reply);
    }
}
