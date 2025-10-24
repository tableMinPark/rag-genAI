package com.genai.application.domain;

import lombok.*;

@Builder
@ToString
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Prompt {

    private String promptCode;

    private String promptName;

    private String context;

    private double temperature;

    private double topP;

    private double minP;

    private double topK;

    private int maxTokens;

    /**
     * 조정 값 수정
     */
    public void setParameter(String context, double temperature, double topP, double minP, double topK, int maxTokens) {
        this.context = context;
        this.temperature = temperature;
        this.topP = topP;
        this.minP = minP;
        this.topK = topK;
        this.maxTokens = maxTokens;
    }
}
