package com.ladiesapparel.wishlist;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WishlistItemRepository extends JpaRepository<WishlistItem, Long> {

    @Query("select wi from WishlistItem wi " +
            "join fetch wi.product p " +
            "where wi.user.id = :userId " +
            "order by wi.addedAt desc")
    List<WishlistItem> findByUserIdWithProductDetails(@Param("userId") Long userId);

    List<WishlistItem> findByUserIdOrderByAddedAtDesc(Long userId);

    Optional<WishlistItem> findByUserIdAndProductId(Long userId, Long productId);

    boolean existsByUserIdAndProductId(Long userId, Long productId);
}