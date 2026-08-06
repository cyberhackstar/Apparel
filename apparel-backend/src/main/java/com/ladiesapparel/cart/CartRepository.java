package com.ladiesapparel.cart;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {

    @Query("select distinct c from Cart c " +
            "left join fetch c.items i " +
            "left join fetch i.productVariant pv " +
            "left join fetch pv.product p " +
            "where c.user.id = :userId")
    Optional<Cart> findByUserIdWithDetails(@Param("userId") Long userId);

    Optional<Cart> findByUserId(Long userId);
}