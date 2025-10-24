package com.genai.application.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@ToString
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Search<T extends Document> {

    private double distance;

    private double score;

    private double weight;

    private T fields;
}
