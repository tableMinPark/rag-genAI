package com.genai.controller.dto.request;

import lombok.*;

@ToString
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LlmChatRequestDto {

    private Long chatId;

    private String sessionId;

    private String query;

    private String context;

    private String prompt;

    private Integer maxTokens;

    private Double temperature;

    private Double topP;
}
