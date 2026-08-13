package com.ladiesapparel.category;

import com.ladiesapparel.category.dto.CategoryRequest;
import com.ladiesapparel.category.dto.CategoryResponse;
import com.ladiesapparel.common.ApiException;
import com.ladiesapparel.common.SlugUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    @Transactional
    @CacheEvict(cacheNames = {"categoryTree", "categoriesFlat"}, allEntries = true)
    public CategoryResponse createCategory(CategoryRequest request) {
        Category parent = resolveParent(request.getParentCategoryId());

        Category category = Category.builder()
                .name(request.getName())
                .slug(generateUniqueSlug(request.getName()))
                .description(request.getDescription())
                .imageUrl(request.getImageUrl())
                .parentCategory(parent)
                .displayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0)
                .active(true)
                .build();

        categoryRepository.save(category);
        return toResponse(category);
    }

    @Transactional
    @CacheEvict(cacheNames = {"categoryTree", "categoriesFlat"}, allEntries = true)
    public CategoryResponse updateCategory(Long id, CategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Category not found"));

        if (!category.getName().equalsIgnoreCase(request.getName())) {
            category.setSlug(generateUniqueSlug(request.getName()));
        }

        category.setName(request.getName());
        category.setDescription(request.getDescription());
        category.setImageUrl(request.getImageUrl());
        category.setDisplayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : category.getDisplayOrder());
        category.setParentCategory(resolveParent(request.getParentCategoryId()));

        categoryRepository.save(category);
        return toResponse(category);
    }

    @Transactional
    @CacheEvict(cacheNames = {"categoryTree", "categoriesFlat"}, allEntries = true)
    public void deactivateCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Category not found"));
        category.setActive(false);
        categoryRepository.save(category);
    }

    @Transactional
    @CacheEvict(cacheNames = { "categoryTree", "categoriesFlat" }, allEntries = true)
    public void activateCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Category not found"));
        category.setActive(true);
        categoryRepository.save(category);
    }

    /** Admin view — includes inactive categories, so admins can actually find and reactivate one. Not cached (admin-only, low traffic). */
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllForAdmin() {
        return categoryRepository.findAllByOrderByDisplayOrderAsc()
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CategoryResponse getById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Category not found"));
        return toResponse(category);
    }

    /** Returns top-level categories with their sub-categories nested (for menus/homepage). */
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "categoryTree")
    public List<CategoryResponse> getCategoryTree() {
        List<Category> topLevel = categoryRepository.findByParentCategoryIsNullAndActiveTrueOrderByDisplayOrderAsc();

        return topLevel.stream()
                .map(cat -> {
                    CategoryResponse response = toResponse(cat);
                    List<Category> children = categoryRepository
                            .findByParentCategoryIdAndActiveTrueOrderByDisplayOrderAsc(cat.getId());
                    response.setSubCategories(children.stream().map(this::toResponse).collect(Collectors.toList()));
                    return response;
                })
                .collect(Collectors.toList());
    }

    /** Flat list — used in admin panel for dropdowns etc. */
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "categoriesFlat")
    public List<CategoryResponse> getAllActiveFlat() {
        return categoryRepository.findByActiveTrueOrderByDisplayOrderAsc()
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    private Category resolveParent(Long parentId) {
        if (parentId == null) {
            return null;
        }
        return categoryRepository.findById(parentId)
                .orElseThrow(() -> ApiException.notFound("Parent category not found"));
    }

    private String generateUniqueSlug(String name) {
        String baseSlug = SlugUtil.toSlug(name);
        String slug = baseSlug;
        while (categoryRepository.existsBySlug(slug)) {
            slug = SlugUtil.withRandomSuffix(baseSlug);
        }
        return slug;
    }

    private CategoryResponse toResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .slug(category.getSlug())
                .description(category.getDescription())
                .imageUrl(category.getImageUrl())
                .parentCategoryId(category.getParentCategory() != null ? category.getParentCategory().getId() : null)
                .parentCategoryName(category.getParentCategory() != null ? category.getParentCategory().getName() : null)
                .displayOrder(category.getDisplayOrder())
                .active(category.isActive())
                .build();
    }
}
