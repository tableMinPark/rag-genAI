package com.genai.client;

import com.genai.application.domain.Document;
import com.genai.application.domain.Search;
import com.genai.application.enums.CollectionType;
import com.genai.application.port.SearchPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class SearchPortTest {

    private static final Logger log = LoggerFactory.getLogger(SearchPortTest.class);

    @Autowired
    private SearchPort searchPort;

    @Test
    @DisplayName("법령 컬렉션 키워드 검색을 한다.")
    void keywordSearchTest() {

        List<Search<Document>> response =
                searchPort.keywordSearch(CollectionType.LAW, CollectionType.LAW.getCollectionId(), "토지 소유권의 범위에 대해서 알려줘", 1, "LAW-TEST-ID");

        assertNotNull(response);

        response.forEach(lawEntityDocumentVo -> {
            log.info("{}", lawEntityDocumentVo.getFields());
        });
    }

    @Test
    @DisplayName("법령 컬렉션 벡터 검색을 한다.")
    void vectorSearchTest() {

        List<Search<Document>> response = searchPort.vectorSearch(CollectionType.LAW, CollectionType.LAW.getCollectionId(), "토지 소유권의 범위에 대해서 알려줘", 1);

        assertNotNull(response);

        response.forEach(lawEntityDocumentVo -> {
            log.info("{}", lawEntityDocumentVo.getFields());
        });
    }
}