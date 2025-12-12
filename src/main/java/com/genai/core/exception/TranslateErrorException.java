package com.genai.core.exception;

public class TranslateErrorException extends RuntimeException {
    public TranslateErrorException(String modelType) {
        super(modelType + " 번역 작업에 실패했습니다.");
    }
}
