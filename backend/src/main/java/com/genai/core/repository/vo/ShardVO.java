package com.genai.core.repository.vo;

import lombok.*;

@ToString
@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ShardVO {

    private String shardName;

    private Integer documentNum;

    private Integer directorySize;
}
