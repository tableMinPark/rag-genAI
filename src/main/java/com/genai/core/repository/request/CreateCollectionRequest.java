package com.genai.core.repository.request;

import com.genai.core.repository.vo.CollectionSettingsVO;
import com.genai.core.repository.vo.IndexFieldVO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.List;
import java.util.Map;

@ToString
@Builder
@Getter
@AllArgsConstructor
public class CreateCollectionRequest {

    private int numOfShards;

    private int numOfReplication;

    private int numOfIndexThreads;

    private String searchRoutingStrategy;

    private int recentQuery;

    private int maxQueryCacheCount;

    private int maxQueryCacheRamBytesUsed;

    private String mappingTableId;

    private List<String> fields;

    private Map<String, IndexFieldVO> indexFields;

    private CollectionSettingsVO advancedSettings;
}
