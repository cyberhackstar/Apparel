package com.ladiesapparel.cart;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {
    Optional<Cart> findByUserId(Long userId);

    /**
     * Eagerly fetches items -> productVariant -> product in one query — used for the read path
     * (getCart()) to avoid N+1 lazy loads. Note: product.images is still lazy-loaded per item,
     * which is fine since getCart() is wrapped in @Transactional(readOnly = true) — the small
     * number of items in a typical cart makes this an acceptable trade-off vs. a 4th join.
     */
    @Query("select distinct c from Cart c " +
            "left join fetch c.items i " +
            "left join fetch i.productVariant pv " +
            "left join fetch pv.product p " +
            "where c.user.id = :userId")
    Optional<Cart> findByUserIdWithDetails(@Param("userId") Long userId);
}
