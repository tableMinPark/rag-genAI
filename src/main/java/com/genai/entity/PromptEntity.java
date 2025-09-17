package com.genai.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Builder
@ToString
@Getter
@AllArgsConstructor
public class PromptEntity {

    private final String promptCode;

    private final String promptName;

    private final String context;
}
