package com.genai.app.chat.controller.dto.request;

import lombok.*;

import javax.validation.constraints.NotBlank;

@ToString
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatSimulationRequestDto {

    @NotBlank
    private String sessionId;

    private String query;

    private String context;

    @NotBlank
    private String promptContent;

    private Integer maxTokens;

    private Double temperature;

    private Double topP;
}
