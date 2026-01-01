package com.genai.core.repository.response;

import com.genai.core.repository.vo.ShardVO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

@ToString
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class GetIndexResponse {

    private int totalDocs;

    private int totalSize;

    private List<ShardVO> enableShards;

    private List<ShardVO> disableShards;
}
