package com.genai.core.service.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@ToString
@Builder
@Getter
@AllArgsConstructor
public class ChatVO {

    private final Long chatId;

    private final String title;
}
