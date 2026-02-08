package com.genai.core.repository;

import com.genai.core.repository.entity.ChunkEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ChunkRepository extends JpaRepository<ChunkEntity, Long> {

    @Query(value = """
        SELECT COUNT(chunk.chunkId)
          FROM ChunkEntity chunk
          JOIN PassageEntity passage ON passage.passageId = chunk.passageId
          JOIN SourceEntity source ON source.sourceId = passage.sourceId
         WHERE source.sourceId IN (:sourceIds)
    """)
    Integer countBySourceIdIn(List<Long> sourceIds);
}
