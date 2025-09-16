package com.genai.client;

import com.genai.client.response.SearchResponse;
import com.genai.entity.LawEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class SearchClientTest {

    private static final Logger log = LoggerFactory.getLogger(SearchClientTest.class);

    @Autowired
    private SearchClient searchClient;

    @Test
    @DisplayName("법령 컬렉션 키워드 검색을 한다.")
    void lawKeywordSearchTest() {

        SearchResponse<LawEntity> response =
                searchClient.lawKeywordSearch("토지 소유권의 범위에 대해서 알려줘", 1, "LAW-TEST-ID");

        assertNotNull(response);

        response.getDocument().forEach(lawEntityDocumentVo -> {
            log.info("{}", lawEntityDocumentVo.getFields());
        });
    }

    @Test
    @DisplayName("법령 컬렉션 벡터 검색을 한다.")
    void lawVectorSearchTest() {

        SearchResponse<LawEntity> response = searchClient.lawVectorSearch("토지 소유권의 범위에 대해서 알려줘", 1);

        assertNotNull(response);

        response.getDocument().forEach(lawEntityDocumentVo -> {
            log.info("{}", lawEntityDocumentVo.getFields());
        });
    }
}