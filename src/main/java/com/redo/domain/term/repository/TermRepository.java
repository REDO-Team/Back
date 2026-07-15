package com.redo.domain.term.repository;

import com.redo.domain.term.entity.Term;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TermRepository extends JpaRepository<Term, Long> {
    // 활성화된(is_active=true) 약관 목록 조회
    List<Term> findByIsActiveTrue();

    List<Term> findByIsRequiredTrue();
}