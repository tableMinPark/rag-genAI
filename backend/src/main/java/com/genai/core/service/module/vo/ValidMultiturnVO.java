package com.genai.core.service.module.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Builder
@Getter
@AllArgsConstructor
public class ValidMultiturnVO {

    private final boolean isChangeTopic;

    private final List<Long> conversationIds;
}
