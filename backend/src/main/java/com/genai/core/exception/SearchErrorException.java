package com.genai.core.exception;

public class SearchErrorException extends RuntimeException {

    public SearchErrorException(String message) {
        super("검색 요청 진행중 오류가 발생했습니다 | " + message);
    }

    public SearchErrorException(String message, Throwable throwable) {
        super("검색 요청 진행중 오류가 발생했습니다 | " + message, throwable);
    }
}
