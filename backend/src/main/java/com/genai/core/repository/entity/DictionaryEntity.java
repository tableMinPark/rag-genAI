package com.genai.core.repository.entity;

import lombok.*;
import org.hibernate.annotations.Comment;

import javax.persistence.*;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "GEN_DICTIONARY")
public class DictionaryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "dictionary_id")
    @Comment("사전 ID")
    private Long dictionaryId;

    @Column(name = "dictionary")
    @Comment("사전")
    private String dictionary;

    @Column(name = "dictionary_desc")
    @Comment("사전 설명")
    private String dictionaryDesc;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "language_code")
    private CommonCodeEntity language;
}
