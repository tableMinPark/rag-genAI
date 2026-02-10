package com.genai.common.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class DocumentVO {

    private final String name;

    private final String ext;

    private final List<DocumentContentVO> documentContents;
}
