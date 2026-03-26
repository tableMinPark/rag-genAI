package com.genai.core.exception;

public class ModelErrorException extends RuntimeException {

    public ModelErrorException(String message) {
        super("모델 요청 처리중 오류가 발생했습니다 | " + message);
    }
}
