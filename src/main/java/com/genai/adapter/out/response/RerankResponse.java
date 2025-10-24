package com.genai.adapter.out.response;

import com.genai.application.domain.Rerank;
import lombok.*;

import java.util.Collections;
import java.util.List;

@ToString
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RerankResponse {

    private String status;

    private String message;

    private List<Rerank> data = Collections.emptyList();
}
