package com.genai.chat.controller.dto.request;

import lombok.*;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@ToString
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatLlmRequestDto {

    @NotNull
    private String sessionId;

    @NotBlank
    private String query;
}
