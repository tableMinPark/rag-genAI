package com.genai.app.chat.service.vo;

import com.genai.core.repository.entity.ChatPassageEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Builder
@Getter
@AllArgsConstructor
public class ChatPassageVO {

    private final Long chatPassageId;

    private final Long msgId;

    private final Long fileDetailId;

    private final String sourceType;

    private final String categoryCode;

    private final String content;

    public static ChatPassageVO of(ChatPassageEntity chatPassageEntity) {
        return ChatPassageVO.builder()
                .chatPassageId(chatPassageEntity.getChatPassageId())
                .msgId(chatPassageEntity.getMsgId())
                .fileDetailId(chatPassageEntity.getFileDetailId())
                .sourceType(chatPassageEntity.getSourceType())
                .categoryCode(chatPassageEntity.getCategoryCode())
                .content(chatPassageEntity.getContent())
                .build();
    }

    public static List<ChatPassageVO> toList(List<ChatPassageEntity> chatPassageEntities) {
        return chatPassageEntities.stream().map(ChatPassageVO::of).toList();
    }
}
