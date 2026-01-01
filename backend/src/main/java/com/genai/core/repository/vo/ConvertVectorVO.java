package com.genai.core.repository.vo;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@ToString
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ConvertVectorVO {

    @JsonAlias("DOCID")
    @JsonProperty("DOCID")
    private String chunkId;

    @JsonAlias("context")
    @JsonProperty("context")
    private String context;

    // 벡터 변환 필드
    @JsonAlias("context_VECTOR")
    @JsonProperty("context_VECTOR")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String contextVector;
}
