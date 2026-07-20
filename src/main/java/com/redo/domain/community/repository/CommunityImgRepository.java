package com.redo.domain.community.repository;

import com.redo.domain.community.entity.Community;
import com.redo.domain.community.entity.CommunityImg;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CommunityImgRepository extends JpaRepository<CommunityImg, Long> {

    Optional<CommunityImg> findFirstByCommunityOrderByDisplayOrderAsc(Community community);

    // 목록 조회용: 여러 게시글의 이미지를 한 번에 조회한다(display_order 오름차순, 게시글별 첫 항목이 대표 이미지).
    List<CommunityImg> findByCommunityInOrderByDisplayOrderAsc(List<Community> communities);
}
