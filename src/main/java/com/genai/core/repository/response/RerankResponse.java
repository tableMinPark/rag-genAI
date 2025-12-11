package com.genai.core.repository.response;

import com.genai.core.repository.wrapper.Rerank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.Collections;
import java.util.List;

@ToString
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RerankResponse {

    private String status;

    private String message;

    private List<Rerank> data = Collections.emptyList();
}
