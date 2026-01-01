package com.genai.core.repository.entity;

import com.genai.core.repository.vo.ShardVO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class IndexEntity {

    private int totalDocs;

    private int totalSize;

    private List<ShardVO> enableShards;

    private List<ShardVO> disableShards;
}
