package com.genai.core.service.business.vo;

import com.genai.core.repository.entity.DocumentEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Builder
@Getter
@AllArgsConstructor
public class EmbedVO {

    private final List<DocumentEntity> documentEntities;

    private final List<String> deleteDocumentIds;
}
