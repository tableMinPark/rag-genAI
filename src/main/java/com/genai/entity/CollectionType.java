package com.genai.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CollectionType {

    LAW("law")
    ;

    private final String collectionName;
}
