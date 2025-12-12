package com.genai.core.service.vo;

import lombok.*;

@ToString
@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AnswerVO {

    private String id;

    private String content;

    private String finishReason;

    private Boolean isInference;

}
