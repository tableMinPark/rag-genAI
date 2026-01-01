package com.genai.report.controller.dto.response;

import lombok.*;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReportResponseDto {

    private String sessionId;

    private long msgId;

    private String content;
}
