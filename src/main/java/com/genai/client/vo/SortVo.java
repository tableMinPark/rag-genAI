package com.genai.client.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SortVo {

    private final String field;

    private final boolean reverse;
}
