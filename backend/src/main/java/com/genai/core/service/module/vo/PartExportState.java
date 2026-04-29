package com.genai.core.service.module.vo;

import com.genai.global.utils.StringUtil;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@ToString
@Getter
public class PartExportState {

    private final List<String> contents;

    private final int contentLength;

    private final String stateId;

    private final AtomicReference<Float> progressAtomic;

    private final float interval;

    private final int round;

    public  static final int    PART_INIT_TOKEN_SIZE = 7000;
    private static final int[]  PART_MAX_TOKEN_SIZES = { 7000, 14000, Integer.MAX_VALUE };

    @Builder
    private PartExportState(List<String> contents, String stateId, AtomicReference<Float> progressAtomic, float interval, int round) {
        this.contents = contents;
        this.stateId = stateId;
        this.progressAtomic = progressAtomic;
        this.interval = interval;
        this.round = round;

        int contentLength = 0;
        for (String content : contents) {
            contentLength += content.length();
        }
        this.contentLength = contentLength;
    }

    public static PartExportState init(List<String> contents) {
        return PartExportState.builder()
                .round(0)
                .contents(contents)
                .stateId(StringUtil.generateRandomId())
                .progressAtomic(new AtomicReference<Float>(0f))
                .interval(1f / (contents.size() * PART_MAX_TOKEN_SIZES.length))
                .build();
    }

    public PartExportState nextRound(List<String> contents) {
        return PartExportState.builder()
                .round(round + 1)
                .contents(contents)
                .stateId(stateId)
                .progressAtomic(progressAtomic)
                .interval(interval)
                .build();
    }

    public float increaseProgress() {
        return progressAtomic.updateAndGet(progress -> progress + interval);
    }

    public float finishProgress() {
        return progressAtomic.updateAndGet(progress -> 1f);
    }

    public boolean isFinished(int contentLength) {
        return PART_MAX_TOKEN_SIZES[round] >= contentLength;
    }
}
