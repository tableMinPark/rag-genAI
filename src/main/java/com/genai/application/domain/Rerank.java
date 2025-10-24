package com.genai.application.domain;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Builder
@ToString
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Rerank {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private double rerankScore;

    @JsonAlias("fields")
    @JsonProperty("fields")
    private Document document;
}
