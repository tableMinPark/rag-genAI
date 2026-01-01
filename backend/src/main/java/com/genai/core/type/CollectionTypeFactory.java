package com.genai.core.type;

import org.springframework.stereotype.Component;

@Component
public class CollectionTypeFactory {

    /**
     * AI 컬렉션
     *
     * @return AI 컬렉션 타입 객체
     */
    public CollectionType ai() {
        return CollectionType.ai();
    }

    /**
     * 나만의 AI 컬렉션 (동적 컬렉션)
     *
     * @return 나만의 AI 컬렉션 타입 객체
     */
    public CollectionType myai() {
        return CollectionType.myai();
    }
}
