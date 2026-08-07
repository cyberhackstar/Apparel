package com.ladiesapparel.product;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

    boolean existsBySku(String sku);

    Optional<ProductVariant> findBySku(String sku);

    @EntityGraph(attributePaths = { "product" }) // ← only this line added
    List<ProductVariant> findByStockQuantityLessThanAndActiveTrueOrderByStockQuantityAsc(int threshold);
}