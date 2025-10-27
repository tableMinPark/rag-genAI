package com.genai.global.enums;

import com.genai.global.constant.SearchConst;
import com.genai.service.domain.Document;
import com.genai.service.domain.DocumentLaw;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public enum CollectionType {

    LAW(DocumentLaw.class, "nhis_law", "법령", "TRAIN-LAW", List.of("title", "sub_title", "third_title", "content"), List.of(SearchConst.VECTOR_FIELD), List.of("score")),
    MANUAL(Document.class, "nhis_manual", "메뉴얼", "TRAIN-MANUAL", List.of("title", "sub_title", "third_title", "content"), List.of(SearchConst.VECTOR_FIELD), List.of("score"))
    ;

    private final Class<?> mappingClass;
    private final String collectionId;
    private final String collectionName;
    private final String alias;
    private final List<String> keywordSearchFields;
    private final List<String> vectorSearchFields;
    private final List<String> sortFields;
}
