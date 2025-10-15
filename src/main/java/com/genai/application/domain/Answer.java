package com.genai.application.domain;

import lombok.*;

@ToString
@Getter
public class Answer {

    private final String id;

    private final String content;

    private final String finishReason;

    private final boolean isInference;

    @Builder
    public Answer(String id, String content, String finishReason, boolean isInference) {
        this.id = id;
        this.content = content;
        this.finishReason = finishReason;
        this.isInference = isInference;
    }

    /**
     * 공백 문자 치환
     * SSE Event 수신 시, 공백 문자 누락 이슈 발생
     */
    public String getConvertContent() {
        String content = this.content;

        if (this.content != null) {
            content = content.replace(" ", "&nbsp");
            content = content.replace("\n", "\\n");
        }

        return content;
    }
}
