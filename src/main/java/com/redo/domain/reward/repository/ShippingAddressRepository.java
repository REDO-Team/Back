package com.redo.domain.reward.repository;

import com.redo.domain.reward.entity.ShippingAddress;
import com.redo.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ShippingAddressRepository extends JpaRepository<ShippingAddress, Long> {

    List<ShippingAddress> findByUserAndDeletedAtIsNullOrderByIsDefaultDescUpdatedAtDescIdDesc(User user);

    Optional<ShippingAddress> findByIdAndUserAndDeletedAtIsNull(Long id, User user);

    Optional<ShippingAddress> findByIdAndDeletedAtIsNull(Long id);

    boolean existsByUserAndDeletedAtIsNull(User user);

    @Modifying
    @Query("""
        update ShippingAddress s
        set s.isDefault = false
        where s.user = :user
          and s.deletedAt is null
        """)
    void clearDefaultByUser(@Param("user") User user);

    @Modifying
    @Query("""
        update ShippingAddress s
        set s.isDefault = false
        where s.user = :user
          and s.id <> :shippingAddressId
          and s.deletedAt is null
        """)
    void clearDefaultByUserExcept(
            @Param("user") User user,
            @Param("shippingAddressId") Long shippingAddressId
    );

    Optional<ShippingAddress> findFirstByUserAndDeletedAtIsNullOrderByUpdatedAtDescIdDesc(User user);
}
