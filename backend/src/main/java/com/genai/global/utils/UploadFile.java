package com.genai.global.utils;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
@AllArgsConstructor
public class UploadFile {

    private final String ip;

    private final String filePath;

    private final String originFileName;

    private final String fileName;

    private final Integer fileSize;

    private final String ext;

    private final String url;
}
