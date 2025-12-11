package com.genai.core.service.vo;

import lombok.*;

@ToString
@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ContextVO {

    private String title;

    private String subTitle;

    private String thirdTitle;

    private String content;

    private String subContent;
}
