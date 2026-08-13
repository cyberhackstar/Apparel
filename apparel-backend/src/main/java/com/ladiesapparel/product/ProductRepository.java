package com.ladiesapparel.product;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    Optional<Product> findBySlugAndActiveTrue(String slug);

    boolean existsBySlug(String slug);

    /**
     * Single-product detail fetch (product detail page). Safe to combine variants + images in
     * one @EntityGraph now that Product.variants/images are Sets, not Lists — Hibernate only
     * throws MultipleBagFetchException for List-typed ("bag") collections fetched together.
     */
    @EntityGraph(attributePaths = {"variants", "images", "category"})
    @Query("select p from Product p where p.slug = :slug and p.active = true")
    Optional<Product> findWithDetailsBySlugAndActiveTrue(@Param("slug") String slug);

    @EntityGraph(attributePaths = {"variants", "images", "category"})
    @Query("select p from Product p where p.id = :id")
    Optional<Product> findWithDetailsById(@Param("id") Long id);

    @Query("select p from Product p where p.category.id = :categoryId and p.active = true and p.id <> :excludeId " +
           "order by p.averageRating desc, p.createdAt desc")
    List<Product> findRelated(@Param("categoryId") Long categoryId, @Param("excludeId") Long excludeId, Pageable pageable);

    List<Product> findByIdInAndActiveTrue(List<Long> ids);

    /**
     * Batch-hydrates variants + images + category for a page of product ids in ONE query,
     * instead of lazy-load queries PER product. Used by search() for paginated listings — the
     * pagination itself is still done as a separate id-only query first (see search()), so this
     * method is only ever called with a small, already-page-sized list of ids, never with a
     * LIMIT/OFFSET of its own — avoiding the classic "collection fetch join breaks pagination"
     * problem entirely.
     */
    @EntityGraph(attributePaths = {"variants", "images", "category"})
    @Query("select distinct p from Product p where p.id in :ids")
    List<Product> findAllWithDetailsByIdIn(@Param("ids") List<Long> ids);
}
