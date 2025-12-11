package com.genai.core.service.vo;

import lombok.*;

@ToString
@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SourceVO {

    private long fileDetailId;

    private String fileOriginName;

    private String url;

    private String ext;

    private String sourceType;

    private String categoryCode;

    private String content;
}
