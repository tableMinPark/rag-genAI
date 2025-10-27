package com.genai.controller.dto.response;

import com.genai.controller.vo.ReferenceDocumentVo;
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

    private List<ReferenceDocumentVo> documents;
}
