package com.genai.chat.controller.dto.response;

import com.genai.core.service.vo.DocumentVo;
import lombok.*;

import java.util.List;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponseDto {

    private String sessionId;

    private String query;

    private List<DocumentVo> documents;
}
