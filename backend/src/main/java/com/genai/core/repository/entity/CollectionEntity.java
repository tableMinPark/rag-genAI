package com.genai.core.repository.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CollectionEntity {

    private String collectionId;

    private int numOfShards;

    private int numOfReplication;

    private List<String> fields;
}
