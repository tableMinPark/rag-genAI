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
@Table(name = "GEN_FILE_DETAIL")
@EntityListeners(AuditingEntityListener.class)
public class FileDetailEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "file_detail_id", nullable = false, updatable = false)
    @Comment("파일 상세 ID")
    private Long fileDetailId;

    @Column(name = "file_id")
    @Comment("파일 ID")
    private Long fileId;

    @Column(name = "file_origin_name")
    @Comment("원본 파일명")
    private String fileOriginName;

    @Column(name = "file_name")
    @Comment("저장 파일명")
    private String fileName;

    @Column(name = "ip")
    @Comment("저장 서버 IP")
    private String ip;

    @Column(name = "file_path")
    @Comment("파일 상대 경로")
    private String filePath;

    @Column(name = "file_size")
    @Comment("파일 크기")
    private Integer fileSize;

    @Column(name = "ext")
    @Comment("파일 확장자")
    private String ext;

    @Column(name = "url")
    @Comment("파일 접근 경로")
    private String url;

    @CreatedDate
    @Column(name = "sys_create_dt")
    @Comment("생성 일자")
    private LocalDateTime sysCreateDt;

    @LastModifiedDate
    @Column(name = "sys_modify_dt")
    @Comment("수정 일자")
    private LocalDateTime sysModifyDt;
}
