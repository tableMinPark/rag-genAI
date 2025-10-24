package com.genai.adapter.in.vo;

import lombok.Builder;

@Builder
public record ReferenceDocumentVo(
    String title,
    String subTitle,
    String thirdTitle,
    String content,
    String subContent,
    String filePath,
    String url,
    String docType,
    String categoryCode
) {}