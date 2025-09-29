package com.genai.application.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
@AllArgsConstructor
public class Context {

    private final String title;

    private final String subTitle;

    private final String thirdTitle;

    private final String content;

    private final String subContent;
}
