package com.genai.global.common.dto;

import lombok.*;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResponseDto<T> {

    private Integer code;

    private String status;

    private String message;

    private T result;
}
