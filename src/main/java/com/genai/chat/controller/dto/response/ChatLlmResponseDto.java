package com.genai.chat.controller.dto.response;

import lombok.*;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatLlmResponseDto {

    private String sessionId;

    private String query;
}
