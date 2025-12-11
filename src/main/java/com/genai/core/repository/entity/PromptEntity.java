package com.genai.core.repository.entity;

import lombok.*;
import org.hibernate.annotations.Comment;

import javax.persistence.*;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "GEN_PROMPT")
public class PromptEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "prompt_id", nullable = false, updatable = false)
    @Comment("프롬프트 ID")
    private Long promptId;

    @Column(name = "prompt_name")
    @Comment("프롬프트명")
    private String promptName;

    @Column(name = "prompt_content")
    @Comment("프롬프트 내용")
    private String promptContent;

    @Column(name = "temperature")
    @Comment("창의성")
    private Float temperature;

    @Column(name = "top_p")
    @Comment("일관성")
    private Float topP;

    @Column(name = "maximum_tokens")
    @Comment("최대 토큰 수")
    private Integer maximumTokens;
}
