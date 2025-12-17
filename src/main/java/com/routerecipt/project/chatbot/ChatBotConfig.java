package com.routerecipt.project.chatbot;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;

/**
 * ✅ OpenAI 클라이언트(OpenAIClient)를 스프링 빈(Bean)으로 등록하는 설정 클래스
 * - 한 번만 만들어서 재사용(컨트롤러/서비스에서 @Autowired로 주입받아 사용)
 */
@Configuration
public class ChatBotConfig {

    /**
     * ✅ openai.api-key 값을 application.properties에서 읽어옴
     * secret.properties(또는 secret 파일)에서 OPENAI_API_KEY를 세팅해두면
     * application.properties가 그 값을 "치환"해서 apiKey 변수에 주입해줌
     * 첫 번째는 Spring이 키 값을 변수에 주입하는 단계
     */
	@Value("${openai.api-key}")
    private String apiKey;

    /**
     * ✅ OpenAIClient Bean 생성
     *
     주요 설정:
     * - apiKey: OpenAI API 사용을 위한 인증 키입니다. 이 키를 통해 요청이 인증됩니다.
     * - build(): 설정된 값들을 바탕으로 클라이언트를 실제 생성합니다.
     * 두 번째는 그 값을 실제 클라이언트 생성에 사용하는 단계입니다.
     */
    @Bean
    public OpenAIClient openAIClient() {
        return OpenAIOkHttpClient.builder()
                .apiKey(apiKey) // ✅ 인증용 API 키 설정
                .build();		// ✅ 클라이언트 생성 완료
    }
}
