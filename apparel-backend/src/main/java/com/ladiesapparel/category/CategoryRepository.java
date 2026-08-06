package com.ladiesapparel.category;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findBySlug(String slug);

    boolean existsBySlug(String slug);

    List<Category> findByParentCategoryIsNullAndActiveTrueOrderByDisplayOrderAsc();

    List<Category> findByParentCategoryIdAndActiveTrueOrderByDisplayOrderAsc(Long parentId);

    List<Category> findByActiveTrueOrderByDisplayOrderAsc();
}
