package com.genai.app.chat.controller.dto.response;

import lombok.*;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatMyAiResponseDto {

    private String sessionId;

    private String query;
}
