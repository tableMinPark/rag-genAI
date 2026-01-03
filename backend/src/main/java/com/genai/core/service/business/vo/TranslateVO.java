package com.genai.core.service.business.vo;

import lombok.*;

@ToString
@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TranslateVO {

    private long chatId;

    private long msgId;

    private String content;
}
