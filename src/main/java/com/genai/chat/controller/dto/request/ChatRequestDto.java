package com.genai.chat.controller.dto.request;

import lombok.*;

@ToString
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequestDto {

    private Long chatId;

    private String sessionId;

    private String query;
}
