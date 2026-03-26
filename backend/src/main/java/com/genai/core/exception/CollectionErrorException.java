package com.genai.core.exception;

public class CollectionErrorException extends RuntimeException {

    public CollectionErrorException(String message) {
        super("컬렉션 작업중 오류가 발생했습니다. | " + message);
    }

    public CollectionErrorException(String message, Throwable throwable) {
        super("컬렉션 작업중 오류가 발생했습니다. | " + message, throwable);
    }
}
