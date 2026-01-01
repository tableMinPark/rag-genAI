package com.genai.core.type;

import com.genai.core.repository.entity.DocumentEntity;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
public class CollectionType {

    private final Class<?> mappingClass;
    private final String collectionId;
    private final List<String> keywordSearchFields;
    private final List<String> vectorSearchFields;
    private final List<String> sortFields;

    @Builder
    private CollectionType(Class<?> mappingClass, String collectionId, List<String> keywordSearchFields, List<String> vectorSearchFields, List<String> sortFields) {
        this.mappingClass = mappingClass;
        this.collectionId = collectionId;
        this.keywordSearchFields = keywordSearchFields;
        this.vectorSearchFields = vectorSearchFields;
        this.sortFields = sortFields;
    }

    /**
     * AI 컬렉션
     *
     * @return AI 컬렉션 타입 객체
     */
    public static CollectionType ai() {
        return CollectionType.builder()
                .mappingClass(DocumentEntity.class)
                .collectionId("nhis_ai")
                .keywordSearchFields(List.of("title", "sub_title", "third_title", "content"))
                .vectorSearchFields(List.of("context"))
                .sortFields(List.of("score"))
                .build();
    }

    /**
     * 나만의 AI 컬렉션 (동적 컬렉션)
     *
     * @return 나만의 AI 컬렉션 타입 객체
     */
    public static CollectionType myai() {
        return CollectionType.builder()
                .mappingClass(DocumentEntity.class)
                .collectionId("gen_myai")
                .keywordSearchFields(List.of("title", "sub_title", "third_title", "content"))
                .vectorSearchFields(List.of("context"))
                .sortFields(List.of("score"))
                .build();
    }
}
