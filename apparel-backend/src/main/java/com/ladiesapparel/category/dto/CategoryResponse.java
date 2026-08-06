package com.ladiesapparel.category.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryResponse {
    private Long id;
    private String name;
    private String slug;
    private String description;
    private String imageUrl;
    private Long parentCategoryId;
    private String parentCategoryName;
    private Integer displayOrder;
    private boolean active;
    private List<CategoryResponse> subCategories; // populated only for tree view
}
