package com.genai.application.vo;

import lombok.*;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReferenceDocumentVo {

    private String title;

    private String subTitle;

    private String thirdTitle;

    private String content;

    private String subContent;

    private String filePath;

    private String url;

    private String docType;

    private String categoryCode;
}
