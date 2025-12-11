package com.genai.core.repository.entity;

import lombok.*;
import org.hibernate.annotations.Comment;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "GEN_MYAI_PROJECT")
@EntityListeners(AuditingEntityListener.class)
public class MyAiProjectEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "project_id")
    @Comment("프로젝트 ID")
    private Long projectId;

    @Column(name = "project_name")
    @Comment("프로젝트명")
    private String projectName;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.REFRESH)
    @JoinColumn(name = "file_id")
    private FileEntity file;


    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "prompt_id")
    private PromptEntity prompt;
}
