package com.genai.adapter.out.vo;

import lombok.*;

//@ToString
//@Getter
//@AllArgsConstructor
//public class SortVo {
//
//    private String field;
//
//    private boolean reverse;
//}

public record SortVo(String field, boolean reverse) {}