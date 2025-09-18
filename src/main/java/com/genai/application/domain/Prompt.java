package com.genai.application.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Builder
@ToString
@Getter
@AllArgsConstructor
public class Prompt {

    private final String promptCode;

    private final String promptName;

    private final String context;

    private final double temperature;

    private final double topP;

    private final int maxTokens;
}
