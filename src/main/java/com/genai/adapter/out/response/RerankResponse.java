package com.genai.adapter.out.response;

import com.genai.application.domain.RerankDocument;
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
public class RerankResponse<T> {

    private String status;

    private String message;

    private List<RerankDocument<T>> data = Collections.emptyList();
}
