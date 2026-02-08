package com.genai.app.summary.controller.dto.response;

import lombok.*;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SummaryResponseDto {

    private String sessionId;

    private long msgId;
}
