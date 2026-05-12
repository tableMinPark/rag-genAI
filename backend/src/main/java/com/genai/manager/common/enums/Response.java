package com.genai.manager.common.enums;

import com.genai.global.common.enums.ApiResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum Response implements ApiResponse {

    ;

    private final HttpStatus statusCode;
    private final int code;
    private final String message;
    private final String status;
}
