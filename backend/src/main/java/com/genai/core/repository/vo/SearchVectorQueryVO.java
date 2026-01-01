package com.genai.core.repository.vo;

import lombok.Builder;

@Builder
public record SearchVectorQueryVO(String field, String queryStr, int k) {}