package com.genai.core.exception;

public class CollectionErrorException extends RuntimeException {
    public CollectionErrorException(String modelType) {
        super(modelType + " 컬렉션 작업에 실패했습니다.");
    }
}
