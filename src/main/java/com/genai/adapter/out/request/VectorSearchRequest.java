package com.genai.adapter.out.request;

import com.genai.adapter.out.vo.VectorQueryVo;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.List;

@ToString
@Getter
public class VectorSearchRequest {

    private final List<VectorQueryVo> vectorQuery;

    @Builder
    public VectorSearchRequest(List<VectorQueryVo> vectorQuery) {
        this.vectorQuery = vectorQuery;
    }
}
