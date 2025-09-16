package com.genai.client.response;

import com.genai.client.vo.DocumentVo;
import com.genai.client.vo.PageVo;
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

    private List<DocumentVo<T>> document = Collections.emptyList();

    private List<String> typoKeyword = Collections.emptyList();

    private double elapsedTime;

    private Map<String, Object> aggregations = Collections.emptyMap();
}
