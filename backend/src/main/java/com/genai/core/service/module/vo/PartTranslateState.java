package com.genai.core.service.module.vo;

import com.genai.common.utils.StringUtil;
import com.genai.common.vo.IndexedContentVO;
import com.genai.core.service.business.vo.DictionaryVO;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@ToString
@Getter
public class PartTranslateState {

    private final List<IndexedContentVO> indexedContents;

    private final String afterLangName;

    private final List<DictionaryVO> dictionaries;

    private final boolean containDic;

    private final String stateId;

    private final AtomicReference<Float> progressAtomic;

    private final float interval;

    @Builder
    public PartTranslateState(List<IndexedContentVO> indexedContents, String afterLangName, List<DictionaryVO> dictionaries, boolean containDic, String stateId, AtomicReference<Float> progressAtomic, float interval) {
        this.indexedContents = indexedContents;
        this.afterLangName = afterLangName;
        this.dictionaries = dictionaries;
        this.containDic = containDic;
        this.stateId = stateId;
        this.progressAtomic = progressAtomic;
        this.interval = interval;
    }

    public static PartTranslateState init(List<IndexedContentVO> indexedContents, String afterLangName, List<DictionaryVO> dictionaries, boolean containDic) {
        return PartTranslateState.builder()
                .indexedContents(indexedContents)
                .afterLangName(afterLangName)
                .dictionaries(dictionaries)
                .containDic(containDic)
                .stateId(StringUtil.generateRandomId())
                .progressAtomic(new AtomicReference<Float>(0f))
                .interval(1f / (indexedContents.size()))
                .build();
    }

    public float increaseProgress() {
        return progressAtomic.updateAndGet(progress -> progress + interval);
    }
}
