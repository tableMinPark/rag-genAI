package com.genai.core.repository.entity;

import lombok.*;
import org.hibernate.annotations.Comment;

import javax.persistence.*;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "GEN_COMN_CODE")
public class ComnCodeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "code_id", nullable = false, updatable = false)
    @Comment("공통 코드 ID")
    private Long codeId;

    @Column(name = "code")
    @Comment("공통 코드")
    private String code;

    @Column(name = "code_name")
    @Comment("공통 코드명")
    private String codeName;

    @Column(name = "code_group")
    @Comment("그룹 코드")
    private String codeGroup;

    @Column(name = "sort_order")
    @Comment("정렬 필드")
    private Integer sortOrder;


}
