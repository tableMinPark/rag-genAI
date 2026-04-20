package com.genai.app.chat.service.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Builder
@Getter
@AllArgsConstructor
public class ChatDetailVO {

    private final Long msgId;

    private final Long chatId;

    private final String query;

    private final String answer;

    private final LocalDateTime sysCreateDt;

    private final LocalDateTime sysModifyDt;

    private final List<ChatPassageVO> passages;
}
