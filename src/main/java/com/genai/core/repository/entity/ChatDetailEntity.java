package com.genai.core.repository.entity;

import lombok.*;
import org.hibernate.annotations.Comment;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "GEN_CHAT_DETAIL")
@EntityListeners(AuditingEntityListener.class)
public class ChatDetailEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "msg_id", nullable = false, updatable = false)
    @Comment("메시지 ID")
    private Long msgId;

    @Column(name = "chat_id")
    @Comment("대화 ID")
    private Long chatId;

    @Column(name = "speaker")
    @Comment("발화자 ID")
    private String speaker;

    @Setter
    @Column(name = "content", length = 4000)
    @Comment("대화 내용")
    private String content;

    @CreatedDate
    @Column(name = "sys_create_dt")
    @Comment("생성 일자")
    private LocalDateTime sysCreateDt;

    @LastModifiedDate
    @Column(name = "sys_modify_dt")
    @Comment("수정 일자")
    private LocalDateTime sysModifyDt;

}
