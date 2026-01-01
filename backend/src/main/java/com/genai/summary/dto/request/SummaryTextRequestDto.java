package com.genai.summary.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SummaryTextRequestDto {

    private String sessionId;

    private float lengthRatio;

    private String context;
}
