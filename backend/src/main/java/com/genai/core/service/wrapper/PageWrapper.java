package com.genai.core.service.wrapper;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Builder
@Getter
@AllArgsConstructor
public class PageWrapper<T> {

    private List<T> content;

    private final boolean isLast;

    private final int pageNo;

    private final int pageSize;

    private final long totalCount;

    private final int totalPages;
}