package com.genai.core.service.module.vo;

import lombok.*;

@ToString
@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CommonCodeVO {

    private Long codeId;

    private String code;

    private String codeName;

    private String codeGroup;

    private Integer sortOrder;
}
