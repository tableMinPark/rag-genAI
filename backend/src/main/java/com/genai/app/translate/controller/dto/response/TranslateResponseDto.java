package com.genai.app.translate.controller.dto.response;

import lombok.*;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TranslateResponseDto {

    private String sessionId;

    private long msgId;
}
