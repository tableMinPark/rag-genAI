package com.genai.client.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PageVo {

    private final int from;

    private final int size;
}
