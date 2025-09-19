package com.genai.adapter.in.dto.response;

import lombok.*;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponseDto {

    private String sessionId;

    private String query;
}
