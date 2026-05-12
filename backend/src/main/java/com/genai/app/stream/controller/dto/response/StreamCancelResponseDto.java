package com.genai.app.stream.controller.dto.response;

import lombok.*;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StreamCancelResponseDto {

    private String sessionId;
}
