package com.genai.app.chat.service.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Builder
@Getter
@AllArgsConstructor
public class ChatVO {

    private final Long chatId;

    private final String state;

    private final String title;

    private final String menuCode;

    private final String sysCreateUser;

    private final LocalDateTime sysCreateDt;

    private final LocalDateTime sysModifyDt;
}
