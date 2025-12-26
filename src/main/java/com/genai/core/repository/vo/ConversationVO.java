package com.genai.core.repository.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
@AllArgsConstructor
public class ConversationVO {

    private final String query;

    private final String answer;
}
