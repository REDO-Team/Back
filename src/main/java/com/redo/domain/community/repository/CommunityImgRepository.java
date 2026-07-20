package com.redo.domain.community.repository;

import com.redo.domain.community.entity.Community;
import com.redo.domain.community.entity.CommunityImg;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CommunityImgRepository extends JpaRepository<CommunityImg, Long> {

    Optional<CommunityImg> findFirstByCommunityOrderByDisplayOrderAsc(Community community);
}
