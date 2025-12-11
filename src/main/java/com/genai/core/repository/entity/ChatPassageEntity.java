package com.genai.core.repository.entity;

import lombok.*;
import org.hibernate.annotations.Comment;

import javax.persistence.*;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "GEN_CHAT_PASSAGE")
public class ChatPassageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chat_passage_id", nullable = false, updatable = false)
    @Comment("대화 패시지 ID")
    private Long chatPassageId;

    @Column(name = "msg_id")
    @Comment("메시지 ID")
    private Long msgId;

    @Column(name = "file_detail_id")
    @Comment("참고 문서 파일 상세 ID")
    private Long fileDetailId;

    @Column(name = "source_type")
    @Comment("문서 타입")
    private String sourceType;

    @Column(name = "category_code")
    @Comment("카테고리 코드")
    private String categoryCode;

    @Column(name = "content", length = 4000)
    @Comment("참고 문서 내용")
    private String content;
}
