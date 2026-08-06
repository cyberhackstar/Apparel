package com.ladiesapparel.product;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    Optional<Product> findBySlugAndActiveTrue(String slug);

    @EntityGraph(attributePaths = { "variants", "images", "category" })
    @Query("select p from Product p where p.slug = :slug and p.active = true")
    Optional<Product> findWithDetailsBySlugAndActiveTrue(@Param("slug") String slug);

    @EntityGraph(attributePaths = { "variants", "images", "category" })
    @Query("select p from Product p where p.id = :id")
    Optional<Product> findWithDetailsById(@Param("id") Long id);

    boolean existsBySlug(String slug);

    @Query("select p from Product p where p.category.id = :categoryId and p.active = true and p.id <> :excludeId " +
            "order by p.averageRating desc, p.createdAt desc")
    List<Product> findRelated(@Param("categoryId") Long categoryId, @Param("excludeId") Long excludeId,
            Pageable pageable);

    List<Product> findByIdInAndActiveTrue(List<Long> ids);
}
