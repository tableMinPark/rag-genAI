package com.genai.global.utils;

import com.genai.core.config.properties.ExtractProperty;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;

@Component
@RequiredArgsConstructor
public class ExtractUtil {

    private final ExtractProperty extractProperty;

    /**
     * SNF 텍스트 추출
     *
     * @param fullPath 파일 경로
     * @return 추출 문자열
     */
    public String extract(String fullPath) {
        StringBuilder contentBuilder = new StringBuilder();

        try {
            String os = System.getProperty("os.name").toLowerCase();

            String[] cmd;
            if (os.contains("windows")) {
                cmd = new String[]{"cmd.exe", "/c", extractProperty.getWindowSnfPath(), "-NO_WITHPAGE", "-C", "utf8", fullPath};
            } else if (os.contains("mac")) {
                cmd = new String[]{extractProperty.getMacSnfPath(), "-NO_WITHPAGE", "-C", "utf8", fullPath};
            } else {
                cmd = new String[]{extractProperty.getLinuxSnfPath(), "-NO_WITHPAGE", "-C", "utf8", fullPath};
            }

            Process process = Runtime.getRuntime().exec(cmd);

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;
            while ((line = reader.readLine()) != null) {
                contentBuilder.append(line).append("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("snf extract error");
        }

        return contentBuilder.toString();
    }

    /**
     * 파일 삭제
     *
     * @param path 파일 경로
     */
    public void removeFile(Path path) {

        boolean isSuccess = path.toFile().exists() && path.toFile().delete();

        if (!isSuccess) {
            throw new RuntimeException("delete file error");
        }
    }
}
