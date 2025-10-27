package com.genai.global.enums;

import com.genai.global.constant.SearchConst;
import com.genai.service.domain.Document;
import com.genai.service.domain.DocumentLaw;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public enum MenuType {

    LAW(DocumentLaw.class, "nhis_law", "법령", List.of("TRAIN-LAW"), "PROM-001", List.of("title", "sub_title", "third_title", "content"), List.of(SearchConst.VECTOR_FIELD), List.of("score")),
    MANUAL(Document.class, "nhis_manual", "메뉴얼", List.of("TRAIN-MANUAL"), "PROM-001", List.of("title", "sub_title", "third_title", "content"), List.of(SearchConst.VECTOR_FIELD), List.of("score"))
    ;

    private final Class<?> mappingClass;
    private final String collectionId;
    private final String menuName;
    private final List<String> alias;
    private final String promptCode;
    private final List<String> keywordSearchFields;
    private final List<String> vectorSearchFields;
    private final List<String> sortFields;
}
