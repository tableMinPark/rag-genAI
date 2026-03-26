package com.genai.core.service.module.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.List;

@ToString
@Builder
@Getter
@AllArgsConstructor
public class MultiturnConversationVO {

    private final List<ConversationVO> conversations;

    private final boolean isChangeTopic;
}
