package com.redo.domain.term.converter;

import com.redo.domain.term.dto.TermResDTO;
import com.redo.domain.term.entity.Term;

import java.util.List;
import java.util.stream.Collectors;

public class TermConverter {

    // Term 하나 → TermInfo 하나로 변환하는 메서드
    public static TermResDTO.TermInfo toTermInfo(Term term) {
        return new TermResDTO.TermInfo(
                term.getId(),
                term.getCode(),
                term.getTitle(),
                term.getContent(),
                term.getIsRequired());
    }

    // Term 리스트 → TermList DTO형으로 변환하는 메서드
    public static TermResDTO.TermList toTermList(List<Term> terms) {
        List<TermResDTO.TermInfo> termInfoList = terms.stream()
                .map(TermConverter::toTermInfo)
                .toList();
        return new TermResDTO.TermList(termInfoList);
    }
}