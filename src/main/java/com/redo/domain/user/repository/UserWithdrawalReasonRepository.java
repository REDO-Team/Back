package com.redo.domain.user.repository;

import com.redo.domain.user.entity.UserWithdrawalReason;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserWithdrawalReasonRepository extends JpaRepository<UserWithdrawalReason, Long> {

    List<UserWithdrawalReason> findByIsActiveTrue();

}