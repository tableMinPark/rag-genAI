package com.genai.core.repository.response;

import com.genai.core.repository.entity.DocumentEntity;
import com.genai.core.repository.vo.SearchPageVO;
import com.genai.core.repository.wrapper.Search;
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
public class SearchResponse<T extends DocumentEntity> {

    private int totalHits;

    private SearchPageVO pagingInfo;

    private List<Search<T>> document = Collections.emptyList();

    private List<String> typoKeyword = Collections.emptyList();

    private double elapsedTime;

    private Map<String, Object> aggregations = Collections.emptyMap();
}
