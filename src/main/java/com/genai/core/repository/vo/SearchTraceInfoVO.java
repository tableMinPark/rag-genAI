package com.genai.core.repository.vo;

import lombok.Builder;

@Builder
public record SearchTraceInfoVO(boolean trace, double timeout) {}