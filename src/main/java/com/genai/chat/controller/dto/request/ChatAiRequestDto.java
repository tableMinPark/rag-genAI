package com.genai.chat.controller.dto.request;

import lombok.*;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

@ToString
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatAiRequestDto {

    @NotNull
    private String sessionId;

    @NotBlank
    private String query;

    private List<String> categoryCodes = new ArrayList<>();
}
