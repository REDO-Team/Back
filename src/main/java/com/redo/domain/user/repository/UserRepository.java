package com.redo.domain.user.repository;

import com.redo.domain.user.entity.User;
import com.redo.domain.user.enums.UserProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    //로그인이 아이디로 유저 찾기
    Optional<User> findByLoginId(String loginId);

    //소셜 로그인시
    Optional<User> findByProviderAndProviderUserId(UserProvider provider, String providerUserId);





}