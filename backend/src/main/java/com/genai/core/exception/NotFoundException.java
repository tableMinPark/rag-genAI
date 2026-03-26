package com.genai.core.exception;

public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message + "을(를) 찾을 수 없습니다.");
    }

    public NotFoundException(String message, Throwable throwable) {
        super(message + "을(를) 찾을 수 없습니다.", throwable);
    }
}
