package com.genai.core.service.business.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
@AllArgsConstructor
public class PrepareVO {

    private final float progress;

    private final String message;
}


