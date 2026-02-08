package com.genai.global.enums;

import com.genai.global.dto.ResponseDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

@Getter
@AllArgsConstructor
public enum Response {

    // STREAM
    CANCEL_STREAM_SUCCESS(HttpStatus.OK, 0, "스트림 중지 요청에 성공했습니다.", ""),

    // CHAT
    CHAT_AI_SUCCESS(HttpStatus.OK, 1000, "AI 답변 요청에 성공했습니다.", ""),
    CHAT_LLM_SUCCESS(HttpStatus.OK, 1001, "LLM 답변 요청에 성공했습니다.", ""),
    CHAT_SIMULATION_SUCCESS(HttpStatus.OK, 1002, "SIMULATION 답변 요청에 성공했습니다.", ""),
    CHAT_MYAI_SUCCESS(HttpStatus.OK, 1003, "나만의 AI 답변 요청에 성공했습니다.", ""),
    CHAT_CATEGORIES_SUCCESS(HttpStatus.OK, 1004, "대화 카테고리 목록 조회에 성공했습니다.", ""),

    // MYAI
    MYAI_GET_PROJECTS_SUCCESS(HttpStatus.OK, 2000, "나만의 AI 프로젝트 목록 조회에 성공했습니다.", ""),
    MYAI_CREATE_PROJECT_SUCCESS(HttpStatus.OK, 2001, "나만의 AI 프로젝트 생성에 성공했습니다.", ""),
    MYAI_DELETE_PROJECT_SUCCESS(HttpStatus.OK, 2002, "나만의 AI 프로젝트 삭제에 성공했습니다.", ""),
    MYAI_GET_PROJECT_SOURCES_SUCCESS(HttpStatus.OK, 2003, "나만의 AI 프로젝트 임베딩 문서 조회에 성공했습니다.", ""),
    MYAI_UPDATE_PROJECT_SOURCES_SUCCESS(HttpStatus.OK, 2004, "나만의 AI 프로젝트 임베딩 문서 수정에 성공했습니다.", ""),


    // REPORT
    REPORT_GENERATE_TEXT_SUCCESS(HttpStatus.OK, 3000, "텍스트 참조 보고서 생성 요청에 성공했습니다.", ""),
    REPORT_GENERATE_FILE_SUCCESS(HttpStatus.OK, 3001, "파일 참조 보고서 생성 요청에 성공했습니다.", ""),

    // SUMMARY
    SUMMARY_GENERATE_TEXT_SUCCESS(HttpStatus.OK, 4000, "텍스트 참조 요약 요청에 성공했습니다.", ""),
    SUMMARY_GENERATE_FILE_SUCCESS(HttpStatus.OK, 4001, "파일 참조 요약 요청에 성공했습니다.", ""),

    // TRANSLATE
    TRANSLATE_GENERATE_TEXT_SUCCESS(HttpStatus.OK, 5000, "텍스트 참조 번역 요청에 성공했습니다.", ""),
    TRANSLATE_GENERATE_FILE_SUCCESS(HttpStatus.OK, 5001, "파일 참조 번역 요청에 성공했습니다.", ""),
    TRANSLATE_TRANSLATE_LANGUAGES(HttpStatus.OK, 5002, "번역 언어 목록 조회에 성공했습니다.", ""),

    // PROMPT
    PROMPT_GET_ROLES_SUCCESS(HttpStatus.OK, 6000, "프롬프트 역할 목록 조회에 성공했습니다.", ""),
    PROMPT_GET_TONES_SUCCESS(HttpStatus.OK, 6001, "프롬프트 답변 톤 목록 조회에 성공했습니다.", ""),
    PROMPT_GET_STYLES_SUCCESS(HttpStatus.OK, 6002, "프롬프트 답변 스타일 목록 조회에 성공했습니다.", ""),
    ;

    private final HttpStatus statusCode;
    private final int code;
    private final String message;
    private final String status;

    public String setStatus(HttpStatus httpStatus, String status) {
        if (httpStatus != HttpStatus.OK) {
            if (status == null || status.isBlank()) {
                return "error";
            } else {
                return status;
            }
        }
        return "success";
    }

    public <T> ResponseDto<Map<String, Object>> toResponseDto() {
        return ResponseDto.<Map<String, Object>>builder()
                .code(this.code)
                .message(this.message)
                .result(Collections.emptyMap())
                .status(setStatus(this.statusCode, this.status))
                .build();
    }

    public <T> ResponseDto<T> toResponseDto(T result) {
        return ResponseDto.<T>builder()
                .code(this.code)
                .message(this.message)
                .result(result)
                .status(setStatus(this.statusCode, this.status))
                .build();
    }

    public <T> ResponseDto<T> toResponseDto(String customMessage, T result) {
        return ResponseDto.<T>builder()
                .code(this.code)
                .message(Optional.ofNullable(customMessage).orElse(this.message))
                .result(result)
                .status(setStatus(this.statusCode, this.status))
                .build();
    }
}
