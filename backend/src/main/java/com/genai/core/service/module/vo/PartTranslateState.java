package com.genai.core.service.module.vo;

import com.genai.common.utils.StringUtil;
import com.genai.common.vo.IndexedContentVO;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@ToString
@Getter
public class PartTranslateState {

    private final List<IndexedContentVO> indexedContents;

    private final String stateId;

    private final AtomicReference<Float> progressAtomic;

    private final float interval;

    @Builder
    public PartTranslateState(List<IndexedContentVO> indexedContents, String stateId, AtomicReference<Float> progressAtomic, float interval) {
        this.indexedContents = indexedContents;
        this.stateId = stateId;
        this.progressAtomic = progressAtomic;
        this.interval = interval;
    }

    public static PartTranslateState init(List<IndexedContentVO> indexedContents) {
        return PartTranslateState.builder()
                .indexedContents(indexedContents)
                .stateId(StringUtil.generateRandomId())
                .progressAtomic(new AtomicReference<Float>(0f))
                .interval(1f / (indexedContents.size()))
                .build();
    }

    public float increaseProgress() {
        return progressAtomic.updateAndGet(progress -> progress + interval);
    }
}
