package com.genai.core.repository.response;

import com.genai.core.repository.vo.CollectionSettingsVO;
import com.genai.core.repository.vo.IndexFieldVO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;
import java.util.Map;

@ToString
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class GetCollectionResponse {

    private String collectionId;

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
