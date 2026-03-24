package com.genai.core.service.module.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@ToString
@Builder
@Getter
@AllArgsConstructor
public class ConversationVO {

    private final Long id;

    private final String query;

    private final String answer;
}
