package com.genai.core.service.module.vo;

import com.genai.global.stream.subscriber.StreamEvent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@ToString
@Builder
@Getter
@AllArgsConstructor
public class PartExportContextVO {

    private final int index;

    private final boolean isLast;

    private final String partExport;

    private final StreamEvent streamEvent;

    public static PartExportContextVO of(int index, boolean isLast, String partExport, String stateId, float progress) {
        return PartExportContextVO.builder()
                .index(index)
                .isLast(isLast)
                .partExport(partExport)
                .streamEvent(StreamEvent.prepare(stateId, progress, "문서 전처리중"))
                .build();
    }
}
