package com.genai.application.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@ToString
@Getter
@AllArgsConstructor
public class Document<T> {

    private final double distance;

    private final double score;

    private final double weight;

    private final T fields;
}
