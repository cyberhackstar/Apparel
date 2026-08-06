package com.ladiesapparel.review;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    Page<Review> findByProductIdAndStatusOrderByCreatedAtDesc(Long productId, ReviewStatus status, Pageable pageable);
    Page<Review> findByStatusOrderByCreatedAtAsc(ReviewStatus status, Pageable pageable);
    boolean existsByProductIdAndUserId(Long productId, Long userId);
    java.util.List<Review> findByProductIdAndStatus(Long productId, ReviewStatus status);
}
