package com.genai.global.enums;

import com.genai.global.constant.SearchConst;
import com.genai.service.domain.DocumentMate;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public enum MenuType {

    MATE(DocumentMate.class, "nhis_mate", "메이트", List.of("TRAIN-LAW", "TRAIN-GUIDE", "TRAIN-MANUAL"), "PROM-003", List.of("title", "sub_title", "third_title", "content"), List.of(SearchConst.VECTOR_FIELD), List.of("score")),
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
