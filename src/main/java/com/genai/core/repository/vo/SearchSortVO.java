package com.genai.core.repository.vo;

import lombok.Builder;

@Builder
public record SearchSortVO(String field, boolean reverse) {}