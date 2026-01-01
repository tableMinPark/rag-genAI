package com.genai.chat.controller.dto.response;

import com.genai.core.service.vo.DocumentVO;
import lombok.*;

import java.util.List;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatAiResponseDto {

    private String sessionId;

    private String query;

    private List<DocumentVO> documents;
}
