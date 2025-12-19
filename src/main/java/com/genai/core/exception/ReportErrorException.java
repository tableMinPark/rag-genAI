package com.genai.core.exception;

public class ReportErrorException extends RuntimeException {
    public ReportErrorException(String modelType) {
        super(modelType + " 보고서 생성 작업에 실패했습니다.");
    }
}
