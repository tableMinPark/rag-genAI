package com.genai.core.service.vo;

import lombok.*;

@ToString
@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class FileDetailVO {

    private Long fileDetailId;

    private String fileOriginName;

    private String ext;

    private Integer fileSize;
}
