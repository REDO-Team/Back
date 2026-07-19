package com.redo.domain.certification.repository;

import com.redo.domain.certification.entity.Certification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CertificationRepository extends JpaRepository<Certification, Long> {
}
