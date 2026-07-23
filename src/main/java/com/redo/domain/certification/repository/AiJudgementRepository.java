package com.redo.domain.certification.repository;

import com.redo.domain.certification.entity.AiJudgement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AiJudgementRepository extends JpaRepository<AiJudgement, Long> {

    Optional<AiJudgement> findTopByCertificationIdOrderByCreatedAtDesc(Long certificationId);
}
