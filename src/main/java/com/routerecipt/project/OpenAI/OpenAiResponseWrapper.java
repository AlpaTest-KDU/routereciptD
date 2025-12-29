package com.routerecipt.project.OpenAI;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Data;

@Data
public class OpenAiResponseWrapper {

    private List<Output> output;

    @Data
    public static class Output {
        private List<Content> content;
    }

    @Data
    public static class Content {
        private String text;
    }

    public OpenAiReceiptResult toResult() {

        if (output == null || output.isEmpty()) {
            throw new IllegalStateException("OpenAI 응답에 output이 없습니다.");
        }

        Output out = output.get(0);
        if (out.getContent() == null || out.getContent().isEmpty()) {
            throw new IllegalStateException("OpenAI 응답에 content가 없습니다.");
        }

        String raw = out.getContent().get(0).getText();
        if (raw == null || raw.isBlank()) {
            throw new IllegalStateException("OpenAI 응답 text가 비어있습니다.");
        }

        try {
            // ✅ ```json 코드블록 제거 (핵심)
            String json = raw
                    .replaceAll("```json", "")
                    .replaceAll("```", "")
                    .trim();

            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(json, OpenAiReceiptResult.class);

        } catch (Exception e) {
            throw new IllegalStateException("OpenAI JSON 파싱 실패: " + raw, e);
        }
    }
}
