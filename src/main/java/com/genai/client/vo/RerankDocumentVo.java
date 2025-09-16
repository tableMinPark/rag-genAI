package com.genai.client.vo;

import lombok.Getter;

@Getter
public class RerankDocumentVo<T> extends DocumentVo<T> {

    private final double rerankScore;

    public RerankDocumentVo(double distance, double score, double weight, T fields, double rerankScore) {
        super(distance, score, weight, fields);
        this.rerankScore = rerankScore;
    }
}
