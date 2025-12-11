package com.genai.core.repository.vo;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@ToString
@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CollectionSettingsVO {

    private int maxFullFlushMergeWaitMillis;

    private int maxBufferedDocs;

    private int totalHitsThreshold;

    @JsonAlias("RAMBufferSizeMB")
    @JsonProperty("RAMBufferSizeMB")
    private int ramBufferSizeMB;
}
