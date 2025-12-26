package com.genai.core.controller.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class PageResponseDto<T> {

    private List<T> content;
    private boolean isLast;
    private int pageNo;
    private int pageSize;
    private long totalCount;
    private int totalPages;

    @Builder
    public PageResponseDto(List<T> content, int pageNo, int pageSize, long totalCount, int totalPages, boolean isLast) {
        this.content = content;
        this.isLast = isLast;
        this.pageNo = pageNo;
        this.pageSize = pageSize;
        this.totalCount = totalCount;
        this.totalPages = totalPages;
    }
}
