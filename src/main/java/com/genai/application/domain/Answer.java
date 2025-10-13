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
}
