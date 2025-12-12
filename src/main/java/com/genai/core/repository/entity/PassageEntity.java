package com.genai.core.repository.entity;

import lombok.*;
import org.hibernate.annotations.Comment;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "GEN_PASSAGE")
@EntityListeners(AuditingEntityListener.class)
public class PassageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "passage_id", nullable = false, updatable = false)
    @Comment("패시지 ID")
    private Long passageId;

    @Column(name = "source_id")
    @Comment("대상 문서 ID")
    private Long sourceId;

    @Column(name = "version")
    @Comment("버전 코드")
    private Long version;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(name = "title", length = 4000)
    @Comment("제목")
    private String title;

    @Column(name = "sub_title", length = 4000)
    @Comment("중제목")
    private String subTitle;

    @Column(name = "third_title", length = 4000)
    @Comment("소제목")
    private String thirdTitle;

    @Lob
    @Column(name = "content")
    @Comment("본문")
    private String content;

    @Lob
    @Column(name = "sub_content")
    @Comment("부가 본문")
    private String subContent;

    @Column(name = "token_size")
    @Comment("본문 토큰 크기")
    private Integer tokenSize;

    @Column(name = "update_state")
    @Comment("변경 이력 코드")
    private String updateState;

    @Column(name = "parent_sort_order")
    @Comment("부모 정렬 필드")
    private Integer parentSortOrder;

    @CreatedDate
    @Column(name = "sys_create_dt")
    @Comment("생성 일자")
    private LocalDateTime sysCreateDt;

    @LastModifiedDate
    @Column(name = "sys_modify_dt")
    @Comment("수정 일자")
    private LocalDateTime sysModifyDt;

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "passage_id")
    private List<ChunkEntity> chunks = new ArrayList<>();
}
