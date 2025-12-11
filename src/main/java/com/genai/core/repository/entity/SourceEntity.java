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
@Table(name = "GEN_SOURCE")
@EntityListeners(AuditingEntityListener.class)
public class SourceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "source_id", nullable = false, updatable = false)
    @Comment("대상 문서 ID")
    private Long sourceId;

    @Column(name = "version")
    @Comment("버전 코드")
    private Long version;

    @Column(name = "source_type")
    @Comment("대상 문서 타입")
    private String sourceType;

    @Column(name = "select_type")
    @Comment("전처리 타입")
    private String selectType;

    @Column(name = "category_code")
    @Comment("대상 문서 분류")
    private String categoryCode;

    @Column(name = "name")
    @Comment("대상 문서명")
    private String name;

//    @Lob
    @Column(name = "content")
    @Comment("대상 문서 본문")
    private String content;

    @Column(name = "collection_id")
    @Comment("색인 테이블 ID")
    private String collectionId;

    @Column(name = "file_detail_id")
    @Comment("파일 상세 ID")
    private Long fileDetailId;

    @Column(name = "max_token_size")
    @Comment("전처리 최대 토큰 크기")
    private Integer maxTokenSize;

    @Column(name = "overlap_size")
    @Comment("전처리 오버랩 크기")
    private Integer overlapSize;

    @Column(name = "is_auto")
    @Comment("자동화 처리 여부")
    private Boolean isAuto;

    @CreatedDate
    @Column(name = "sys_create_dt")
    @Comment("생성 일자")
    private LocalDateTime sysCreateDt;

    @LastModifiedDate
    @Column(name = "sys_modify_dt")
    @Comment("수정 일자")
    private LocalDateTime sysModifyDt;

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "source_id")
    private List<PassageEntity> passages = new ArrayList<>();
}
