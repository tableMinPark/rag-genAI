package com.genai.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CollectionType {

    LAW("nhis_proto")
    ;

    private final String collectionName;
}
