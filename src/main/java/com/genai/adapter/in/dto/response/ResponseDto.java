package com.genai.adapter.in.dto.response;

import lombok.*;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResponseDto<T> {

    private String status;

    private String message;

    private T data;
}
