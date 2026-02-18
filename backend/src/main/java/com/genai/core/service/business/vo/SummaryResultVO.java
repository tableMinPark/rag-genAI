package com.genai.core.service.business.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
@AllArgsConstructor
public class SummaryResultVO {

    private String type;

    private String content;

    public static SummaryResultVO ratio(String content) {
        return SummaryResultVO.builder()
                .type("ratio")
                .content(content)
                .build();
    }

    public static SummaryResultVO full(String content) {
        return SummaryResultVO.builder()
                .type("full")
                .content(content)
                .build();
    }
}
