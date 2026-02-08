package com.genai.app.chat.controller.dto.request;

import lombok.*;

import javax.validation.constraints.NotBlank;

@ToString
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatLlmRequestDto {

    @NotBlank
    private String sessionId;

    @NotBlank
    private String query;
}
