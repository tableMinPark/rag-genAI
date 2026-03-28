package com.genai.core.repository;

import com.genai.core.repository.entity.DictionaryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DictionaryRepository extends JpaRepository<DictionaryEntity, Long> {

    List<DictionaryEntity> findByLanguageCode(String languageCode);
}
