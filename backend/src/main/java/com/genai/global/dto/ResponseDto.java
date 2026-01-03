package com.genai.global.dto;

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
