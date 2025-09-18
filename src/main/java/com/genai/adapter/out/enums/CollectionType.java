package com.genai.adapter.out.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public enum CollectionType {

    LAW("nhis_proto", "법령", List.of("context"), List.of("context"), List.of("score"))
    ;

    private final String collectionId;
    private final String collectionName;
    private final List<String> keywordSearchFields;
    private final List<String> vectorSearchFields;
    private final List<String> sortFields;
}
