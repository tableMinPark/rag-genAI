package com.genai.exception;

public class SearchErrorException extends RuntimeException {
    public SearchErrorException(String searchType) {
        super(searchType + " 검색에 실패했습니다.");
    }
}
