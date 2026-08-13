package com.ladiesapparel.product;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class ProductSpecification {

    private ProductSpecification() {
    }

    /**
     * @param active null = no filter (admin "All"), true = active only (storefront), false = inactive only
     */
    public static Specification<Product> build(Long categoryId,
                                                BigDecimal minPrice,
                                                BigDecimal maxPrice,
                                                String size,
                                                String color,
                                                String keyword,
                                                Boolean active) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (active != null) {
                predicates.add(cb.equal(root.get("active"), active));
            }

            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            }

            if (minPrice != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("basePrice"), minPrice));
            }

            if (maxPrice != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("basePrice"), maxPrice));
            }

            if (keyword != null && !keyword.isBlank()) {
                String likePattern = "%" + keyword.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), likePattern),
                        cb.like(cb.lower(root.get("description")), likePattern),
                        cb.like(cb.lower(root.get("brand")), likePattern)
                ));
            }

            // size/color live on the variants — join only when needed, and de-dupe results
            if ((size != null && !size.isBlank()) || (color != null && !color.isBlank())) {
                Join<Product, ProductVariant> variantJoin = root.join("variants", JoinType.INNER);
                if (size != null && !size.isBlank()) {
                    predicates.add(cb.equal(cb.lower(variantJoin.get("size")), size.toLowerCase()));
                }
                if (color != null && !color.isBlank()) {
                    predicates.add(cb.equal(cb.lower(variantJoin.get("color")), color.toLowerCase()));
                }
                query.distinct(true);
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
