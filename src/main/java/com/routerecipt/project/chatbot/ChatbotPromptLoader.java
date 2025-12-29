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

@Component
public class ChatbotPromptLoader {

    private static final Logger log = LoggerFactory.getLogger(ChatbotPromptLoader.class);
    private static final String TOKEN = "{{ROUTERECEIPT_INFO}}";

    @Value("classpath:prompts/routereceipt_info.txt")
    private Resource infoFile;

    @Value("classpath:prompts/system_message_template.txt")
    private Resource templateFile;

    // 기동 시 1회 로드된 최종 System 메시지
    private volatile String cachedSystemMessage;

    @PostConstruct
    public void init() {
        this.cachedSystemMessage = buildSystemMessageInternal();
        log.info("Chatbot system prompt loaded. length={}", cachedSystemMessage.length());
    }

    /** 컨트롤러/서비스에서는 이것만 호출 */
    public String buildSystemMessage() {
        return cachedSystemMessage;
    }

    private String buildSystemMessageInternal() {
        String info = readUtf8Trim(infoFile, "routereceipt_info.txt");
        String template = readUtf8Trim(templateFile, "system_message_template.txt");

        if (template.isEmpty()) {
            log.warn("System message template is empty or missing.");
            return "당신은 routereceipt 팀이 만든 공식 안내 챗봇입니다. 현재 시스템 메시지 템플릿을 불러오지 못했습니다.";
        }

        if (!template.contains(TOKEN)) {
            log.warn("Template token '{}' not found in template file.", TOKEN);
        }

        if (info.isEmpty()) {
            log.warn("Routereceipt info is empty or missing.");
            info = "routereceipt 안내 문서를 불러오지 못했습니다. 추후 다시 시도해 주세요.";
        }

        return template.replace(TOKEN, info);
    }

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
