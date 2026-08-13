package com.ladiesapparel.category;

import com.ladiesapparel.category.dto.CategoryRequest;
import com.ladiesapparel.category.dto.CategoryResponse;
import com.ladiesapparel.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    // ---------- Admin endpoints ----------

    @PostMapping("/api/admin/categories")
    public ResponseEntity<ApiResponse<CategoryResponse>> create(@Valid @RequestBody CategoryRequest request) {
        CategoryResponse response = categoryService.createCategory(request);
        return ResponseEntity.ok(ApiResponse.success("Category created", response));
    }

    @PutMapping("/api/admin/categories/{id}")
    public ResponseEntity<ApiResponse<CategoryResponse>> update(@PathVariable Long id,
                                                                 @Valid @RequestBody CategoryRequest request) {
        CategoryResponse response = categoryService.updateCategory(id, request);
        return ResponseEntity.ok(ApiResponse.success("Category updated", response));
    }

    @DeleteMapping("/api/admin/categories/{id}")
    public ResponseEntity<ApiResponse<Object>> deactivate(@PathVariable Long id) {
        categoryService.deactivateCategory(id);
        return ResponseEntity.ok(ApiResponse.success("Category deactivated"));
    }

    @PatchMapping("/api/admin/categories/{id}/activate")
    public ResponseEntity<ApiResponse<Object>> activate(@PathVariable Long id) {
        categoryService.activateCategory(id);
        return ResponseEntity.ok(ApiResponse.success("Category activated"));
    }

    @GetMapping("/api/admin/categories")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> listAllForAdmin() {
        // shows EVERY category, active or not — the old version only returned active ones,
        // meaning a deactivated category (and its subcategories) had no way to be found again
        return ResponseEntity.ok(ApiResponse.success("Categories fetched", categoryService.getAllForAdmin()));
    }

    @GetMapping("/api/admin/categories/{id}")
    public ResponseEntity<ApiResponse<CategoryResponse>> getOne(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Category fetched", categoryService.getById(id)));
    }

    // ---------- Public endpoints ----------

    @GetMapping("/api/public/categories")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> publicTree() {
        return ResponseEntity.ok(ApiResponse.success("Categories fetched", categoryService.getCategoryTree()));
    }
}
