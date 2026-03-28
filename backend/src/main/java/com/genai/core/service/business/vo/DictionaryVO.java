package com.genai.core.service.business.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
@AllArgsConstructor
public class DictionaryVO {

    private final String dictionary;

    private final String dictionaryDesc;
}
