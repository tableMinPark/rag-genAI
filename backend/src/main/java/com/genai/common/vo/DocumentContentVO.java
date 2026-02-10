package com.genai.common.vo;

import com.genai.common.utils.StringUtil;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.Collections;
import java.util.List;

@Builder
@Getter
@AllArgsConstructor
public class DocumentContentVO {

    private final String contentId;

    private final String compareText;

    private String prefix;

    private String title;

    private String simpleTitle;

    private String context;

    private final List<DocumentContentVO> subDocumentContents;

    private final LineType type;

    public enum LineType {
        TEXT, TABLE, IMAGE,
    }

    public static DocumentContentVO text(String context) {
        return DocumentContentVO.builder()
                .contentId(StringUtil.generateRandomId())
                .compareText(context)
                .context(context)
                .subDocumentContents(Collections.emptyList())
                .type(LineType.TEXT)
                .build();
    }

    public static DocumentContentVO table(String context) {
        return DocumentContentVO.builder()
                .contentId(StringUtil.generateRandomId())
                .compareText(context)
                .context(context)
                .subDocumentContents(Collections.emptyList())
                .type(LineType.TABLE)
                .build();
    }

    public static DocumentContentVO image(String context) {
        return DocumentContentVO.builder()
                .contentId(StringUtil.generateRandomId())
                .compareText(context)
                .context(context)
                .subDocumentContents(Collections.emptyList())
                .type(LineType.IMAGE)
                .build();
    }
}
