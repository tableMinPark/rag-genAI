package com.genai.core.repository.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.genai.core.repository.vo.SearchVectorQueryVO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.List;

@ToString
@Builder
@Getter
@AllArgsConstructor
public class VectorSearchRequest {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final String filterQuery;

    private final List<SearchVectorQueryVO> vectorQuery;
}
