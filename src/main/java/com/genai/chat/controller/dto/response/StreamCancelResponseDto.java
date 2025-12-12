package com.genai.chat.controller.dto.response;

import lombok.*;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StreamCancelResponseDto {

    private String sessionId;
}
