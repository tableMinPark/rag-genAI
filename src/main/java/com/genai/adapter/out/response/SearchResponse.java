package com.genai.adapter.out.response;

import com.genai.application.domain.Document;
import com.genai.application.domain.Search;
import com.genai.adapter.out.vo.PageVo;
import lombok.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@ToString
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SearchResponse<T extends Document> {

    private int totalHits;

    private PageVo pagingInfo;

    private List<Search<T>> document = Collections.emptyList();

    private List<String> typoKeyword = Collections.emptyList();

    private double elapsedTime;

    private Map<String, Object> aggregations = Collections.emptyMap();
}
