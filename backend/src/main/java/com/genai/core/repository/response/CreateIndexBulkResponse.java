package com.genai.core.repository.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@ToString
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CreateIndexBulkResponse {

    private Integer took;

    private Boolean errors;
}
