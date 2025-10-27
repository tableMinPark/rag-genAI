package com.genai.controller.dto.request;

import lombok.*;

@ToString
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LawChatRequestDto {

    private String sessionId;

    private String query;
}
