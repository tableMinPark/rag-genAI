package com.genai.common.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.nio.file.Path;

@ToString
@Builder
@Getter
@AllArgsConstructor
public class HwpImageVO {

    private final String id;

    private final String content;

    private final Path path;

    private final String ext;
}