package com.genai.core.service.vo;

import lombok.Builder;

@Builder
public record DocumentVO(
    String title,
    String subTitle,
    String thirdTitle,
    String content,
    String subContent,
    String originFileName,
    String url,
    String categoryCode,
    String sourceType,
    String ext
) {}