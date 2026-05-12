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
public class PartTranslateContextVO {

    private final int index;

    private final String partTranslate;

    private final StreamEvent streamEvent;

    public static PartTranslateContextVO of(int index, String partTranslate, String stateId, float progress) {
        return PartTranslateContextVO.builder()
                .index(index)
                .partTranslate(partTranslate)
                .streamEvent(StreamEvent.prepare(stateId, progress, "문서 전처리중"))
                .build();
    }
}
