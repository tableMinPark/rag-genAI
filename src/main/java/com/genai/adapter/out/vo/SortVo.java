package com.genai.adapter.out.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@ToString
@Getter
@AllArgsConstructor
public class SortVo {

    private final String field;

    private final boolean reverse;
}
