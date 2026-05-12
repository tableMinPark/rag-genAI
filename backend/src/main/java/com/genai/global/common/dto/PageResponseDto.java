package com.genai.global.common.dto;

import lombok.*;

import java.util.List;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PageResponseDto<T> {

    private List<T> content;
    private boolean isLast;
    private int pageNo;
    private int pageSize;
    private long totalCount;
    private int totalPages;
}
