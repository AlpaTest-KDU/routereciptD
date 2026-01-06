package com.routerecipt.project.chatbot;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import jakarta.annotation.PostConstruct;

/**
 * 챗봇 System Prompt 로더
 *
 * 역할:
 *  - classpath의 프롬프트 텍스트 파일을 읽음
 *  - 템플릿 + 정보 파일을 결합하여 System 메시지 생성
 *  - 애플리케이션 기동 시 1회 로드 후 캐싱
 *
 * 사용 위치:
 *  - ChatBotService 에서 system 메시지 생성 시 사용
 */
@Component
public class ChatbotPromptLoader {

    private static final Logger log = LoggerFactory.getLogger(ChatbotPromptLoader.class);
    /** 템플릿 내 치환 토큰 */
    private static final String TOKEN = "{{ROUTERECEIPT_INFO}}";

    /** 서비스 정보 텍스트 파일 */
    @Value("classpath:prompts/routereceipt_info.txt")
    private Resource infoFile;

    /** System 메시지 템플릿 파일 */
    @Value("classpath:prompts/system_message_template.txt")
    private Resource templateFile;

    /**
     * 기동 시 1회 생성되는 최종 System 메시지
     * - 이후에는 변경되지 않음
     * - volatile: 모든 스레드에서 최신 값 보장
     */
    private volatile String cachedSystemMessage;

    
    /**
     * Spring Bean 초기화 완료 후 자동 실행
     * - System 메시지를 미리 생성하여 캐싱
     */
    @PostConstruct
    public void init() {
        this.cachedSystemMessage = buildSystemMessageInternal();
        log.info("Chatbot system prompt loaded. length={}", cachedSystemMessage.length());
    }

    /**
     * 외부(Controller / Service)에서 사용하는 메서드
     * - 캐싱된 System 메시지를 그대로 반환
     */
    public String buildSystemMessage() {
        return cachedSystemMessage;
    }
    
    /**
     * 실제 System 메시지 조립 로직
     *
     * @return 최종 System 메시지 문자열
     */
    private String buildSystemMessageInternal() {
    	// 서비스 안내 정보 로드
        String info = readUtf8Trim(infoFile, "routereceipt_info.txt");
        
        // System 메시지 템플릿 로드
        String template = readUtf8Trim(templateFile, "system_message_template.txt");

        // 템플릿이 비어 있으면 최소 안전 메시지 반환
        if (template.isEmpty()) {
            log.warn("System message template is empty or missing.");
            return "당신은 routereceipt 팀이 만든 공식 안내 챗봇입니다. 현재 시스템 메시지 템플릿을 불러오지 못했습니다.";
        }
        
        
        // 토큰 누락 경고
        if (!template.contains(TOKEN)) {
            log.warn("Template token '{}' not found in template file.", TOKEN);
        }

        // 서비스 정보 누락 시 대체 문구
        if (info.isEmpty()) {
            log.warn("Routereceipt info is empty or missing.");
            info = "routereceipt 안내 문서를 불러오지 못했습니다. 추후 다시 시도해 주세요.";
        }
        
        // 템플릿에 정보 삽입
        return template.replace(TOKEN, info);
    }

    /**
     * UTF-8 텍스트 파일을 읽어서 trim 후 반환
     *
     * @param resource 읽을 리소스
     * @param nameForLog 로그용 파일명
     * @return 파일 내용 (실패 시 빈 문자열)
     */
    private String readUtf8Trim(Resource resource, String nameForLog) {
        try {
            if (resource == null) {
                log.warn("Resource is null: {}", nameForLog);
                return "";
            }
            if (!resource.exists()) {
                log.warn("Resource does not exist: {}", nameForLog);
                return "";
            }
            String text = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
            return text == null ? "" : text.trim();
        } catch (IOException e) {
            log.warn("Failed to read resource: {}", nameForLog, e);
            return ""; 
            
        }
    }
}
