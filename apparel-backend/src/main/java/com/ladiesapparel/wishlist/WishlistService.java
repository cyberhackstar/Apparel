package com.ladiesapparel.wishlist;

import com.ladiesapparel.auth.AuthenticatedUserProvider;
import com.ladiesapparel.auth.User;
import com.ladiesapparel.common.ApiException;
import com.ladiesapparel.product.Product;
import com.ladiesapparel.product.ProductImage;
import com.ladiesapparel.product.ProductRepository;
import com.ladiesapparel.wishlist.dto.WishlistItemResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WishlistService {

        private final WishlistItemRepository wishlistItemRepository;
        private final ProductRepository productRepository;
        private final AuthenticatedUserProvider authenticatedUserProvider;

        @Transactional
        public void addToWishlist(Long productId) {
                User user = authenticatedUserProvider.getCurrentUser();

                if (wishlistItemRepository.existsByUserIdAndProductId(user.getId(), productId)) {
                        return; // already in wishlist — no-op, keeps the endpoint idempotent
                }

                Product product = productRepository.findById(productId)
                                .orElseThrow(() -> ApiException.notFound("Product not found"));

                WishlistItem item = WishlistItem.builder()
                                .user(user)
                                .product(product)
                                .build();

                wishlistItemRepository.save(item);
        }

        @Transactional
        public void removeFromWishlist(Long productId) {
                User user = authenticatedUserProvider.getCurrentUser();

                WishlistItem item = wishlistItemRepository.findByUserIdAndProductId(user.getId(), productId)
                                .orElseThrow(() -> ApiException.notFound("Item not found in wishlist"));

                wishlistItemRepository.delete(item);
        }

        @Transactional(readOnly = true)
        public List<WishlistItemResponse> getWishlist() {
                User user = authenticatedUserProvider.getCurrentUser();

                return wishlistItemRepository.findByUserIdWithProductDetails(user.getId()).stream()
                                .map(item -> toResponse(item.getProduct()))
                                .collect(Collectors.toList());
        }

        private WishlistItemResponse toResponse(Product product) {
                String imageUrl = product.getImages().stream()
                                .filter(ProductImage::isPrimary)
                                .findFirst()
                                .or(() -> product.getImages().stream()
                                                .min(Comparator.comparing(ProductImage::getDisplayOrder)))
                                .map(ProductImage::getImageUrl)
                                .orElse(null);

                boolean inStock = product.getVariants().stream()
                                .anyMatch(v -> v.isActive() && v.getStockQuantity() != null
                                                && v.getStockQuantity() > 0);

                return WishlistItemResponse.builder()
                                .productId(product.getId())
                                .name(product.getName())
                                .slug(product.getSlug())
                                .imageUrl(imageUrl)
                                .basePrice(product.getBasePrice())
                                .mrp(product.getMrp())
                                .discountPercentage(product.getDiscountPercentage())
                                .inStock(inStock)
                                .build();
        }
}