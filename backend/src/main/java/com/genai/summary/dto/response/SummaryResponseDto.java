package com.genai.summary.dto.response;

import lombok.*;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SummaryResponseDto {

    private String sessionId;

    private long msgId;

    private String content;
}
