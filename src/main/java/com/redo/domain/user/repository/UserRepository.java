package com.redo.domain.user.repository;

import com.redo.domain.user.entity.User;
import com.redo.domain.user.enums.UserProvider;
import com.redo.domain.user.enums.UserStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    long countByStatus(UserStatus status);

    //로그인이 아이디로 유저 찾기
    Optional<User> findByLoginId(String loginId);

    //소셜 로그인시
    Optional<User> findByProviderAndProviderUserId(UserProvider provider, String providerUserId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.id = :userId")
    Optional<User> findByIdForUpdate(@Param("userId") Long userId);


    Optional<User> findByEmail(String email);

}
