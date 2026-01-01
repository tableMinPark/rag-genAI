package com.genai.core.repository;

import com.genai.core.repository.entity.DictionaryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DictionaryRepository extends JpaRepository<DictionaryEntity, Long> {
}
