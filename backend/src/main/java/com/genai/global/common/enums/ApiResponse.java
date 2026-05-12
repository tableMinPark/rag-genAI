package com.genai.global.common.enums;

import com.genai.global.common.dto.ResponseDto;
import org.springframework.http.HttpStatus;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

public interface ApiResponse {

    HttpStatus getStatusCode();

    int getCode();

    String getMessage();

    String getStatus();

    default String setStatus(HttpStatus httpStatus, String status) {
        if (httpStatus != HttpStatus.OK) {
            if (status == null || status.isBlank()) {
                return "error";
            } else {
                return status;
            }
        }
        return "success";
    }

    default <T> ResponseDto<Map<String, Object>> toResponseDto() {
        return ResponseDto.<Map<String, Object>>builder()
                .code(this.getCode())
                .message(this.getMessage())
                .result(Collections.emptyMap())
                .status(setStatus(this.getStatusCode(), this.getStatus()))
                .build();
    }

    default <T> ResponseDto<T> toResponseDto(T result) {
        return ResponseDto.<T>builder()
                .code(this.getCode())
                .message(this.getMessage())
                .result(result)
                .status(setStatus(this.getStatusCode(), this.getStatus()))
                .build();
    }

    default <T> ResponseDto<T> toResponseDto(String customMessage, T result) {
        return ResponseDto.<T>builder()
                .code(this.getCode())
                .message(Optional.ofNullable(customMessage).orElse(this.getMessage()))
                .result(result)
                .status(setStatus(this.getStatusCode(), this.getStatus()))
                .build();
    }
}
