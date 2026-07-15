package com.redo.domain.term.service;

import com.redo.domain.term.converter.TermConverter;
import com.redo.domain.term.dto.TermResDTO;
import com.redo.domain.term.entity.Term;
import com.redo.domain.term.exception.TermErrorCode;
import com.redo.domain.term.repository.TermRepository;
import com.redo.global.apiPayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TermService {

    private final TermRepository termRepository;

    @Transactional
    public TermResDTO.TermList getTerms() {
        List<Term> terms = termRepository.findByIsActiveTrue();
        if(terms.isEmpty()){
            throw new GeneralException(TermErrorCode.TERM_NOT_FOUND);
        }
        return TermConverter.toTermList(terms);
    }
}