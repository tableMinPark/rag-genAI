package com.genai.core.repository.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.util.List;

@ToString
@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class IndexFieldVO {

    private String type;

    private Boolean deduplicate;

    private String sortType;

    private List<String> analyzers;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String format;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String model;
}
