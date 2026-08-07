package com.redo.domain.contribution.repository;

import com.redo.domain.contribution.entity.ContributionEvent;
import com.redo.domain.user.enums.UserStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ContributionEventRepository extends JpaRepository<ContributionEvent, Long> {

    Optional<ContributionEvent> findByCertificationId(Long certificationId);

    @Query("""
            select ce.id
            from ContributionEvent ce
            join ce.user u
            where u.status = :userStatus
              and ce.id = (
                  select max(latestCe.id)
                  from ContributionEvent latestCe
                  where latestCe.user = ce.user
              )
              and (:cursor is null or ce.id < :cursor)
            order by ce.id desc
            """)
    List<Long> findFeedEventIds(
            @Param("cursor") Long cursor,
            @Param("userStatus") UserStatus userStatus,
            Pageable pageable
    );

    @Query("""
            select ce
            from ContributionEvent ce
            join fetch ce.user u
            where u.status = :userStatus
              and ce.id in :eventIds
            order by ce.id desc
            """)
    List<ContributionEvent> findFeedEventsByIdIn(
            @Param("eventIds") Collection<Long> eventIds,
            @Param("userStatus") UserStatus userStatus
    );
}
