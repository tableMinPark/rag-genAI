package com.genai.core.repository.wrapper;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.genai.core.repository.entity.DocumentEntity;
import lombok.*;

@ToString
@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Search<T extends DocumentEntity> {

    @JsonProperty("_score")
    private double score;

    @JsonProperty("_source")
    private T fields;
}
