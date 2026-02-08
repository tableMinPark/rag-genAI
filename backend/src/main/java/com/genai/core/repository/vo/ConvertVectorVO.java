package com.genai.core.repository.vo;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

@ToString
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ConvertVectorVO {

    @JsonAlias("id")
    @JsonProperty("id")
    private Long id;

    @JsonAlias("content")
    @JsonProperty("content")
    private String content;

    // 벡터 변환 필드
    @JsonAlias("vector")
    @JsonProperty("vector")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<Float> vector;
}
