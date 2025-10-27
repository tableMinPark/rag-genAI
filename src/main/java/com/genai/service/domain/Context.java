package com.genai.service.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Context {

    private String title;

    private String subTitle;

    private String thirdTitle;

    private String content;

    private String subContent;
}
