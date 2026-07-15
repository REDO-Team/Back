package com.redo.domain.term.dto;

import java.util.List;

public class TermResDTO {

    // 약관 목록 전체 응답
    public record TermList(
            List<TermInfo> terms
    ) {}

    // 약관 하나의 정보
    public record TermInfo(
            Long termId,
            String code,
            String title,
            String content,
            Boolean isRequired
    ) {}
}