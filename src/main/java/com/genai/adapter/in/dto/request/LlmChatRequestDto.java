package com.genai.adapter.in.dto.request;

import lombok.*;

@ToString
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LlmChatRequestDto {

    private String sessionId;

    private String query;

    private String context;

    private String prompt;
}
