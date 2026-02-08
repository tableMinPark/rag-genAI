package com.genai.app.chat.controller.dto.request;

import lombok.*;

import javax.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.List;

@ToString
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatAiRequestDto {

    @NotBlank
    private String sessionId;

    @NotBlank
    private String query;

    private List<String> categoryCodes = new ArrayList<>();
}
