package com.ladiesapparel.product;

import com.ladiesapparel.category.Category;
import com.ladiesapparel.category.CategoryRepository;
import com.ladiesapparel.common.ApiException;
import com.ladiesapparel.common.PagedResponse;
import com.ladiesapparel.common.SlugUtil;
import com.ladiesapparel.media.CloudinaryService;
import com.ladiesapparel.product.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductImageRepository imageRepository;
    private final CategoryRepository categoryRepository;
    private final CloudinaryService cloudinaryService;

    @Transactional
    @CacheEvict(cacheNames = "productDetail", allEntries = true)
    public ProductResponse createProduct(ProductRequest request) {
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> ApiException.notFound("Category not found"));

        for (ProductVariantRequest v : request.getVariants()) {
            if (variantRepository.existsBySku(v.getSku())) {
                throw ApiException.conflict("SKU already exists: " + v.getSku());
            }
        }

        Product product = Product.builder()
                .name(request.getName())
                .slug(generateUniqueSlug(request.getName()))
                .description(request.getDescription())
                .category(category)
                .brand(request.getBrand())
                .fabric(request.getFabric())
                .basePrice(request.getBasePrice())
                .mrp(request.getMrp())
                .discountPercentage(request.getDiscountPercentage() != null ? request.getDiscountPercentage() : BigDecimal.ZERO)
                .gstPercentage(request.getGstPercentage() != null ? request.getGstPercentage() : new BigDecimal("5.00"))
                .tags(request.getTags())
                .active(true)
                .build();

        Set<ProductVariant> variants = request.getVariants().stream()
                .map(v -> ProductVariant.builder()
                        .product(product)
                        .size(v.getSize())
                        .color(v.getColor())
                        .sku(v.getSku())
                        .stockQuantity(v.getStockQuantity() != null ? v.getStockQuantity() : 0)
                        .additionalPrice(v.getAdditionalPrice() != null ? v.getAdditionalPrice() : BigDecimal.ZERO)
                        .active(true)
                        .build())
                .collect(Collectors.toSet());

        product.setVariants(variants);
        productRepository.save(product);

        return toResponse(product);
    }

    @Transactional
    @CacheEvict(cacheNames = "productDetail", allEntries = true)
    public ProductResponse updateProduct(Long productId, ProductRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> ApiException.notFound("Product not found"));

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> ApiException.notFound("Category not found"));

        if (!product.getName().equalsIgnoreCase(request.getName())) {
            product.setSlug(generateUniqueSlug(request.getName()));
        }

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setCategory(category);
        product.setBrand(request.getBrand());
        product.setFabric(request.getFabric());
        product.setBasePrice(request.getBasePrice());
        product.setMrp(request.getMrp());
        product.setDiscountPercentage(request.getDiscountPercentage() != null ? request.getDiscountPercentage() : product.getDiscountPercentage());
        product.setGstPercentage(request.getGstPercentage() != null ? request.getGstPercentage() : product.getGstPercentage());
        product.setTags(request.getTags());

        // Note: variant updates (stock/price) are handled via dedicated endpoints below,
        // not overwritten wholesale here, to avoid wiping out existing SKUs/stock by mistake.

        productRepository.save(product);
        return toResponse(product);
    }

    @Transactional
    @CacheEvict(cacheNames = "productDetail", allEntries = true)
    public void deactivateProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> ApiException.notFound("Product not found"));
        product.setActive(false);
        productRepository.save(product);
    }

    @Transactional
    @CacheEvict(cacheNames = "productDetail", allEntries = true)
    public void activateProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> ApiException.notFound("Product not found"));
        product.setActive(true);
        productRepository.save(product);
    }

    @Transactional(readOnly = true)
    public ProductResponse getById(Long id) {
        Product product = productRepository.findWithDetailsById(id)
                .orElseThrow(() -> ApiException.notFound("Product not found"));
        return toResponse(product);
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "productDetail", key = "#slug")
    public ProductResponse getBySlug(String slug) {
        Product product = productRepository.findWithDetailsBySlugAndActiveTrue(slug)
                .orElseThrow(() -> ApiException.notFound("Product not found"));
        return toResponse(product);
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getRelated(Long productId, int limit) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> ApiException.notFound("Product not found"));

        return productRepository
                .findRelated(product.getCategory().getId(), productId, org.springframework.data.domain.PageRequest.of(0, limit))
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    /** Batch fetch by id — used by the storefront's "Recently Viewed" strip (client tracks ids in localStorage). */
    @Transactional(readOnly = true)
    public List<ProductResponse> getByIds(List<Long> ids) {
        return productRepository.findByIdInAndActiveTrue(ids).stream().map(this::toResponse).collect(Collectors.toList());
    }

    /**
     * @param active null = no filter (admin "All" view), true = active only (storefront), false = inactive only (admin)
     */
    @Transactional(readOnly = true)
    public PagedResponse<ProductResponse> search(Long categoryId,
                                                  BigDecimal minPrice,
                                                  BigDecimal maxPrice,
                                                  String size,
                                                  String color,
                                                  String keyword,
                                                  Boolean active,
                                                  Pageable pageable) {

        var spec = ProductSpecification.build(categoryId, minPrice, maxPrice, size, color, keyword, active);
        Page<Product> idPage = productRepository.findAll(spec, pageable);

        List<Long> orderedIds = idPage.getContent().stream().map(Product::getId).collect(Collectors.toList());

        if (orderedIds.isEmpty()) {
            return PagedResponse.<ProductResponse>builder()
                    .content(List.of())
                    .pageNumber(idPage.getNumber())
                    .pageSize(idPage.getSize())
                    .totalElements(idPage.getTotalElements())
                    .totalPages(idPage.getTotalPages())
                    .last(idPage.isLast())
                    .build();
        }

        // One batch query hydrates variants + images + category together (N+1 fix). Pagination
        // itself already happened above via the id-only query, so this call never carries its
        // own LIMIT/OFFSET — sidesteps the classic "collection fetch join breaks pagination" issue.
        Map<Long, Product> productsById = productRepository.findAllWithDetailsByIdIn(orderedIds).stream()
                .collect(Collectors.toMap(Product::getId, p -> p));

        List<ProductResponse> content = orderedIds.stream()
                .map(id -> toResponse(productsById.get(id)))
                .collect(Collectors.toList());

        return PagedResponse.<ProductResponse>builder()
                .content(content)
                .pageNumber(idPage.getNumber())
                .pageSize(idPage.getSize())
                .totalElements(idPage.getTotalElements())
                .totalPages(idPage.getTotalPages())
                .last(idPage.isLast())
                .build();
    }

    // ---------- Variant stock management ----------

    @Transactional
    @CacheEvict(cacheNames = "productDetail", allEntries = true)
    public void addVariant(Long productId, ProductVariantRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> ApiException.notFound("Product not found"));

        if (variantRepository.existsBySku(request.getSku())) {
            throw ApiException.conflict("SKU already exists: " + request.getSku());
        }

        ProductVariant variant = ProductVariant.builder()
                .product(product)
                .size(request.getSize())
                .color(request.getColor())
                .sku(request.getSku())
                .stockQuantity(request.getStockQuantity() != null ? request.getStockQuantity() : 0)
                .additionalPrice(request.getAdditionalPrice() != null ? request.getAdditionalPrice() : BigDecimal.ZERO)
                .active(true)
                .build();

        product.getVariants().add(variant);
        productRepository.save(product);
    }

    @Transactional
    @CacheEvict(cacheNames = {"productDetail", "lowStock"}, allEntries = true)
    public void updateStock(Long variantId, int newStockQuantity) {
        ProductVariant variant = variantRepository.findById(variantId)
                .orElseThrow(() -> ApiException.notFound("Variant not found"));

        if (newStockQuantity < 0) {
            throw ApiException.badRequest("Stock cannot be negative");
        }
        variant.setStockQuantity(newStockQuantity);
        variantRepository.save(variant);
    }

    // ---------- Image management ----------

    @Transactional
    @CacheEvict(cacheNames = "productDetail", allEntries = true)
    public ProductImageResponse uploadImage(Long productId, MultipartFile file, boolean isPrimary) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> ApiException.notFound("Product not found"));

        CloudinaryService.UploadResult result = cloudinaryService.upload(file, "products/" + productId);

        if (isPrimary) {
            // unset any existing primary image
            product.getImages().forEach(img -> img.setPrimary(false));
        }

        ProductImage image = ProductImage.builder()
                .product(product)
                .imageUrl(result.url())
                .publicId(result.publicId())
                .displayOrder(product.getImages().size())
                .primary(isPrimary)
                .build();

        product.getImages().add(image);
        productRepository.save(product);

        return ProductImageResponse.builder()
                .id(image.getId())
                .imageUrl(image.getImageUrl())
                .displayOrder(image.getDisplayOrder())
                .primary(image.isPrimary())
                .build();
    }

    @Transactional
    @CacheEvict(cacheNames = "productDetail", allEntries = true)
    public void deleteImage(Long imageId) {
        ProductImage image = imageRepository.findById(imageId)
                .orElseThrow(() -> ApiException.notFound("Image not found"));

        cloudinaryService.delete(image.getPublicId());
        imageRepository.delete(image);
    }

    // ---------- Helpers ----------

    private String generateUniqueSlug(String name) {
        String baseSlug = SlugUtil.toSlug(name);
        String slug = baseSlug;
        while (productRepository.existsBySlug(slug)) {
            slug = SlugUtil.withRandomSuffix(baseSlug);
        }
        return slug;
    }

    private ProductResponse toResponse(Product product) {
        return toResponse(product, product.getImages());
    }

    /** Used by search()'s batch-fetch path — images are passed in explicitly to avoid a lazy-load N+1 per product. */
    private ProductResponse toResponse(Product product, Collection<ProductImage> images) {
        List<ProductVariantResponse> variantResponses = product.getVariants().stream()
                .map(v -> ProductVariantResponse.builder()
                        .id(v.getId())
                        .size(v.getSize())
                        .color(v.getColor())
                        .sku(v.getSku())
                        .stockQuantity(v.getStockQuantity())
                        .additionalPrice(v.getAdditionalPrice())
                        .finalPrice(product.getBasePrice().add(v.getAdditionalPrice()))
                        .active(v.isActive())
                        .inStock(v.getStockQuantity() != null && v.getStockQuantity() > 0)
                        .build())
                .collect(Collectors.toList());

        List<ProductImageResponse> imageResponses = images.stream()
                .sorted((a, b) -> Integer.compare(a.getDisplayOrder(), b.getDisplayOrder()))
                .map(img -> ProductImageResponse.builder()
                        .id(img.getId())
                        .imageUrl(img.getImageUrl())
                        .displayOrder(img.getDisplayOrder())
                        .primary(img.isPrimary())
                        .build())
                .collect(Collectors.toList());

        List<String> tagList = product.getTags() == null || product.getTags().isBlank()
                ? new ArrayList<>()
                : Arrays.stream(product.getTags().split(",")).map(String::trim).collect(Collectors.toList());

        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .slug(product.getSlug())
                .description(product.getDescription())
                .categoryId(product.getCategory().getId())
                .categoryName(product.getCategory().getName())
                .brand(product.getBrand())
                .fabric(product.getFabric())
                .basePrice(product.getBasePrice())
                .mrp(product.getMrp())
                .discountPercentage(product.getDiscountPercentage())
                .gstPercentage(product.getGstPercentage())
                .tags(tagList)
                .averageRating(product.getAverageRating())
                .ratingCount(product.getRatingCount())
                .active(product.isActive())
                .variants(variantResponses)
                .images(imageResponses)
                .build();
    }
}
