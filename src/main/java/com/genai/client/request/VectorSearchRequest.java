package com.genai.client.request;

import com.genai.client.vo.VectorQueryVo;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class VectorSearchRequest {

    private final List<VectorQueryVo> vectorQuery;

    public VectorSearchRequest(String query, int topK) {
        this.vectorQuery = List.of(new VectorQueryVo("context", query, topK));
    }
}
