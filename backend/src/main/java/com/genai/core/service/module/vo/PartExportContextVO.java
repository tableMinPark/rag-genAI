package com.genai.core.service.module.vo;

import com.genai.core.service.business.subscriber.StreamEvent;
import com.genai.core.service.business.vo.PrepareVO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

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
                .streamEvent(StreamEvent.prepare(stateId, PrepareVO.builder()
                                .progress(progress)
                                .message("문서 전처리중")
                        .build()))
                .build();
    }
}
