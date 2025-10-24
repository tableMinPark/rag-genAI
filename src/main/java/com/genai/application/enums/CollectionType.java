package com.genai.application.enums;

import com.genai.application.domain.Document;
import com.genai.application.domain.DocumentLaw;
import com.genai.constant.SearchConst;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public enum CollectionType {

    AI(Document.class, "nhis_ai", "AI", List.of("title", "sub_title", "third_title", "content"), List.of(SearchConst.VECTOR_FIELD), List.of("score")),
    LAW(DocumentLaw.class, "nhis_law", "법령", List.of("title", "sub_title", "third_title", "content"), List.of(SearchConst.VECTOR_FIELD), List.of("score")),
    MANUAL(DocumentLaw.class, "nhis_", "메뉴얼", List.of("title", "sub_title", "third_title", "content"), List.of(SearchConst.VECTOR_FIELD), List.of("score"))
    ;

    private final Class<?> mappingClass;
    private final String collectionId;
    private final String collectionName;
    private final List<String> keywordSearchFields;
    private final List<String> vectorSearchFields;
    private final List<String> sortFields;
}
