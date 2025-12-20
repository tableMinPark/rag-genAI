package com.genai.translate.controller;

import com.genai.core.controller.dto.response.ResponseDto;
import com.genai.core.service.ComnCodeCoreService;
import com.genai.core.service.TranslateCoreService;
import com.genai.core.service.vo.ComnCodeVO;
import com.genai.core.service.vo.TranslateVO;
import com.genai.translate.controller.dto.request.TranslateFileRequestDto;
import com.genai.translate.controller.dto.request.TranslateTextRequestDto;
import com.genai.translate.controller.dto.response.GetTranslateLanguageResponseDto;
import com.genai.translate.controller.dto.response.TranslateResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.util.List;

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/translate")
public class TranslateController {

    private final TranslateCoreService translateCoreService;
    private final ComnCodeCoreService comnCodeCoreService;

    /**
     * 번역 요청
     *
     * @param translateTextRequestDto 번역 요청 정보
     */
    @PostMapping(value = "/text")
    public ResponseEntity<ResponseDto<TranslateResponseDto>> translateText(@Valid @RequestBody TranslateTextRequestDto translateTextRequestDto) {

        String sessionId = translateTextRequestDto.getSessionId();
        String beforeLang = translateTextRequestDto.getBeforeLang();
        String afterLang = translateTextRequestDto.getAfterLang();
        boolean containDic = translateTextRequestDto.isContainDic();
        String context = translateTextRequestDto.getContext();

        log.info("텍스트 번역 요청 : {} | {} -> {} : {}", sessionId, beforeLang, afterLang, context);

        long chatId = 4L;
        TranslateVO translateVO = translateCoreService.translate(beforeLang, afterLang, context, sessionId, chatId, containDic);

        return ResponseEntity.ok()
                .body(ResponseDto.<TranslateResponseDto>builder()
                        .status("SUCCESS")
                        .message("번역 요청 성공")
                        .data(TranslateResponseDto.builder()
                                .sessionId(sessionId)
                                .msgId(translateVO.getMsgId())
                                .content(translateVO.getContent())
                                .build())
                        .build());
    }

    /**
     * 번역 요청
     *
     * @param translateFileRequestDto 번역 요청 정보
     * @param multipartFile 번역 문서 파일
     */
    @PostMapping(value = "/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResponseDto<TranslateResponseDto>> translateFile(
            @Valid
            @RequestPart("requestDto") TranslateFileRequestDto translateFileRequestDto,
            @RequestPart("uploadFile") MultipartFile multipartFile
    ) {

        String sessionId = translateFileRequestDto.getSessionId();
        String beforeLang = translateFileRequestDto.getBeforeLang();
        String afterLang = translateFileRequestDto.getAfterLang();
        boolean containDic = translateFileRequestDto.isContainDic();

        log.info("파일 번역 요청 : {} | {} -> {} : {}", sessionId, beforeLang, afterLang, multipartFile.getOriginalFilename());

        long chatId = 4L;
        TranslateVO translateVO = translateCoreService.translate(beforeLang, afterLang, multipartFile, sessionId, chatId, containDic);

        return ResponseEntity.ok()
                .body(ResponseDto.<TranslateResponseDto>builder()
                        .status("SUCCESS")
                        .message("번역 요청 성공")
                        .data(TranslateResponseDto.builder()
                                .sessionId(sessionId)
                                .msgId(translateVO.getMsgId())
                                .content(translateVO.getContent())
                                .build())
                        .build());
    }

    /**
     * 번역 언어 목록 조회
     */
    @GetMapping("/language")
    public ResponseEntity<ResponseDto<List<GetTranslateLanguageResponseDto>>> getTranslateLanguages() {

        List<ComnCodeVO> translateLanguageComnCodes = comnCodeCoreService.getComnCodes("LANG");

        return ResponseEntity.ok()
                .body(ResponseDto.<List<GetTranslateLanguageResponseDto>>builder()
                        .status("SUCCESS")
                        .message("번역 언어 목록 조회 성공")
                        .data(GetTranslateLanguageResponseDto.toList(translateLanguageComnCodes))
                        .build());
    }
}
