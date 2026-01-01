package com.genai.core.repository.vo;

import lombok.Builder;

@Builder
public record SearchPageVO(int from, int size) {}