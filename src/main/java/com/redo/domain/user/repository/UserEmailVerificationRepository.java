package com.redo.domain.user.repository;

import com.redo.domain.user.entity.UserEmailVerification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserEmailVerificationRepository extends JpaRepository<UserEmailVerification, Long> {

    Optional<UserEmailVerification> findTopByEmailOrderByCreatedAtDesc(String email);

    Optional<UserEmailVerification> findTopByEmailAndVerifiedAtIsNotNullOrderByCreatedAtDesc(String email);
}