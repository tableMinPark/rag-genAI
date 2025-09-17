package com.genai.client.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@ToString
@Getter
@AllArgsConstructor
public class TraceInfoVo {

    private final boolean trace;

    private final double timeout;
}
