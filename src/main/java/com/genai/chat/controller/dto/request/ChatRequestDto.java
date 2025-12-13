package com.genai.chat.controller.dto.request;

import lombok.*;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@ToString
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequestDto {

    @NotNull
    @Min(0)
    private Long chatId;

    @NotNull
    private String sessionId;

    @NotBlank
    private String query;

    private String context;

    private String prompt;

    private Integer maxTokens;

    private Double temperature;

    private Double topP;
}
