package com.ladiesapparel.product;

import com.ladiesapparel.common.ApiResponse;
import com.ladiesapparel.common.PagedResponse;
import com.ladiesapparel.product.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;

@RestController
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final BulkUploadService bulkUploadService;

    // ---------- Admin endpoints ----------

    @PostMapping(value = "/api/admin/products/bulk-upload", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<BulkUploadResult>> bulkUpload(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.success("Bulk upload processed", bulkUploadService.uploadCsv(file)));
    }

    @PostMapping("/api/admin/products")
    public ResponseEntity<ApiResponse<ProductResponse>> create(@Valid @RequestBody ProductRequest request) {
        ProductResponse response = productService.createProduct(request);
        return ResponseEntity.ok(ApiResponse.success("Product created", response));
    }

    @PutMapping("/api/admin/products/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> update(@PathVariable Long id,
                                                                @Valid @RequestBody ProductRequest request) {
        ProductResponse response = productService.updateProduct(id, request);
        return ResponseEntity.ok(ApiResponse.success("Product updated", response));
    }

    @DeleteMapping("/api/admin/products/{id}")
    public ResponseEntity<ApiResponse<Object>> deactivate(@PathVariable Long id) {
        productService.deactivateProduct(id);
        return ResponseEntity.ok(ApiResponse.success("Product deactivated"));
    }

    @GetMapping("/api/admin/products/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> getOneForAdmin(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Product fetched", productService.getById(id)));
    }

    @PostMapping("/api/admin/products/{id}/variants")
    public ResponseEntity<ApiResponse<Object>> addVariant(@PathVariable Long id,
                                                           @Valid @RequestBody ProductVariantRequest request) {
        productService.addVariant(id, request);
        return ResponseEntity.ok(ApiResponse.success("Variant added"));
    }

    @PatchMapping("/api/admin/variants/{variantId}/stock")
    public ResponseEntity<ApiResponse<Object>> updateStock(@PathVariable Long variantId,
                                                            @RequestParam int quantity) {
        productService.updateStock(variantId, quantity);
        return ResponseEntity.ok(ApiResponse.success("Stock updated"));
    }

    @PostMapping(value = "/api/admin/products/{id}/images", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<ProductImageResponse>> uploadImage(@PathVariable Long id,
                                                                          @RequestParam("file") MultipartFile file,
                                                                          @RequestParam(defaultValue = "false") boolean isPrimary) {
        ProductImageResponse response = productService.uploadImage(id, file, isPrimary);
        return ResponseEntity.ok(ApiResponse.success("Image uploaded", response));
    }

    @DeleteMapping("/api/admin/products/images/{imageId}")
    public ResponseEntity<ApiResponse<Object>> deleteImage(@PathVariable Long imageId) {
        productService.deleteImage(imageId);
        return ResponseEntity.ok(ApiResponse.success("Image deleted"));
    }

    // ---------- Public endpoints ----------

    @GetMapping("/api/public/products/{slug}")
    public ResponseEntity<ApiResponse<ProductResponse>> getBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(ApiResponse.success("Product fetched", productService.getBySlug(slug)));
    }

    @GetMapping("/api/public/products/{id}/related")
    public ResponseEntity<ApiResponse<java.util.List<ProductResponse>>> getRelated(
            @PathVariable Long id, @RequestParam(defaultValue = "8") int limit) {
        return ResponseEntity.ok(ApiResponse.success("Related products fetched", productService.getRelated(id, limit)));
    }

    @GetMapping("/api/public/products/batch")
    public ResponseEntity<ApiResponse<java.util.List<ProductResponse>>> getByIds(@RequestParam java.util.List<Long> ids) {
        return ResponseEntity.ok(ApiResponse.success("Products fetched", productService.getByIds(ids)));
    }

    @GetMapping("/api/public/products")
    public ResponseEntity<ApiResponse<PagedResponse<ProductResponse>>> search(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String size,
            @RequestParam(required = false) String color,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int pageSize,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, pageSize, sort);

        PagedResponse<ProductResponse> result = productService.search(
                categoryId, minPrice, maxPrice, size, color, keyword, true, pageable);

        return ResponseEntity.ok(ApiResponse.success("Products fetched", result));
    }
}
