package com.genai.core.service.module.vo;

import lombok.*;

@ToString
@Builder
@Getter
@AllArgsConstructor
public class CommonCodeVO {

    private String code;

    private String codeName;

    private String codeGroup;

    private Integer sortOrder;
}
