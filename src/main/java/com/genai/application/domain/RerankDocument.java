package com.genai.application.domain;

import lombok.Getter;
import lombok.ToString;

@ToString
@Getter
public class RerankDocument<T> extends Document<T> {

    private final double rerankScore;

    public RerankDocument(double distance, double score, double weight, T fields, double rerankScore) {
        super(distance, score, weight, fields);
        this.rerankScore = rerankScore;
    }
}
