package com.genai.client.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TraceInfoVo {

    private final boolean trace;

    private final double timeout;
}
