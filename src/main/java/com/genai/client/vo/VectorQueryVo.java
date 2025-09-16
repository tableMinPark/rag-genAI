package com.genai.client.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class VectorQueryVo {

    private final String field;

    private final String queryStr;

    private final int k;
}
