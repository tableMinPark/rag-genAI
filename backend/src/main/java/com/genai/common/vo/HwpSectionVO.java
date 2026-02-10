package com.genai.common.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
@AllArgsConstructor
public class HwpSectionVO {

    private final String id;

    private final String content;
}