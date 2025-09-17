package com.genai.client.response;

import com.genai.client.vo.RerankDocumentVo;
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

    private List<RerankDocumentVo<T>> data = Collections.emptyList();
}
