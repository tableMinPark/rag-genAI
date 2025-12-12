package com.genai.core.exception;

public class ModelErrorException extends RuntimeException {
    public ModelErrorException(String modelType) {
        super(modelType + " 모델 호출에 실패했습니다.");
    }
}
