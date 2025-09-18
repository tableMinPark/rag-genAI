package com.genai.adapter.out.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@ToString
@Getter
@AllArgsConstructor
public class PageVo {

    private final int from;

    private final int size;
}
