package com.redo.domain.term.repository;

import com.redo.domain.term.entity.Mapping.UserTermsAgreement;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserTermsAgreementRepository extends JpaRepository<UserTermsAgreement, Long> {
}