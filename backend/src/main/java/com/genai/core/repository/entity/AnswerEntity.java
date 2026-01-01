package com.genai.core.repository.entity;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AnswerEntity {

    private String id;

    private String content;

    private String finishReason;

    private Boolean isInference;

    @Builder
    public AnswerEntity(String id, String content, String finishReason, Boolean isInference) {
        this.id = id;
        this.content = content == null ? "" : content;
        this.finishReason = finishReason == null ? "" : finishReason;
        this.isInference = isInference;
    }
}
