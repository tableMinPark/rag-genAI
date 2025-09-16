package com.genai.client.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@ToString
@Getter
@AllArgsConstructor
public class DocumentVo<T> {

    private final double distance;

    private final double score;

    private final double weight;

    private final T fields;
}
