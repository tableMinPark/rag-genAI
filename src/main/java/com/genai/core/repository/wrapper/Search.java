package com.genai.core.repository.wrapper;

import com.genai.core.repository.entity.DocumentEntity;
import lombok.*;

@ToString
@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Search<T extends DocumentEntity> {

    private double distance;

    private double score;

    private double weight;

    private T fields;
}
