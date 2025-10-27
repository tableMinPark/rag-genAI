package com.genai.repository.request;

import com.genai.repository.vo.VectorQueryVo;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.List;

@ToString
@Getter
public class VectorSearchRequest {

    private final String filterQuery;

    private final List<VectorQueryVo> vectorQuery;

    @Builder
    public VectorSearchRequest(String filterQuery, List<VectorQueryVo> vectorQuery) {
        this.filterQuery = filterQuery ==  null ? "" : filterQuery;
        this.vectorQuery = vectorQuery;
    }
}
