package com.routerecipt.project.chatbot;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

@Component
public class ChatbotPromptLoader {
	
	 @Value("classpath:prompts/routereceipt_info.txt")
	 private Resource infoFile;

	 @Value("classpath:prompts/system_message_template.txt")
	 private Resource templateFile;
	 
	    // 캐싱된 결과(완성된 System 메시지)
	    private String cachedSystemMessage;
	 
	 public String buildSystemMessage() {
		    String info = readUtf8Trim(infoFile);
		    String template = readUtf8Trim(templateFile);

		    if (template.isEmpty()) {
		        return "당신은 routereceipt 팀이 만든 공식 안내 챗봇입니다. 현재 시스템 메시지 템플릿을 불러오지 못했습니다.";
		    }

		    if (info.isEmpty()) {
		        info = "routereceipt 안내 문서를 불러오지 못했습니다. 추후 다시 시도해 주세요.";
		    }

		    String result = template.replace("{{ROUTERECEIPT_INFO}}", info);
	        return result;
		}

	    private String readUtf8Trim(Resource resource) {
	        try {
	            if (resource == null || !resource.exists()) return "";
	            String text = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
	            return text == null ? "" : text.trim();
	        } catch (IOException e) {
	            return "";
	        }
	    }
	 
}
