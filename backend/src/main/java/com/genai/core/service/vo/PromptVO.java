package com.genai.core.service.vo;

import lombok.*;

@ToString
@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PromptVO {

    private long promptId;

    private String promptName;

    private long prmptLabelId;

    private String promptLabelName;

    private String promptContent;

    private double temperature;

    private double topP;

    private int maximumTokens;
}
