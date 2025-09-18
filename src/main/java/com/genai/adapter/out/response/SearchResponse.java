package com.genai.adapter.out.response;

import com.genai.application.domain.Document;
import com.genai.adapter.out.vo.PageVo;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@ToString
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SearchResponse<T> {

    private int totalHits;

    private PageVo pagingInfo;

    private List<Document<T>> document = Collections.emptyList();

    private List<String> typoKeyword = Collections.emptyList();

    private double elapsedTime;

    private Map<String, Object> aggregations = Collections.emptyMap();
}
