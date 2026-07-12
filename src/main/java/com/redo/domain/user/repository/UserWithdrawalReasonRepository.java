package com.redo.domain.user.repository;

import com.redo.domain.user.entity.UserWithdrawalReason;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserWithdrawalReasonRepository extends JpaRepository<UserWithdrawalReason, Long> {

    List<UserWithdrawalReason> findByIsActiveTrue();

    //비활성화 탈퇴사유 방지 메서드
    Optional<UserWithdrawalReason> findByIdAndIsActiveTrue(Long id);

}