package com.genai.adapter.out.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@ToString
@Getter
@AllArgsConstructor
public class VectorQueryVo {

    private final String field;

    private final String queryStr;

    private final int k;
}
