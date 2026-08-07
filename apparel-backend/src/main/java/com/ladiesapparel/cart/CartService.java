package com.ladiesapparel.cart;

import com.ladiesapparel.auth.AuthenticatedUserProvider;
import com.ladiesapparel.auth.User;
import com.ladiesapparel.cart.dto.AddToCartRequest;
import com.ladiesapparel.cart.dto.CartItemResponse;
import com.ladiesapparel.cart.dto.CartResponse;
import com.ladiesapparel.common.ApiException;
import com.ladiesapparel.product.ProductImage;
import com.ladiesapparel.product.ProductVariant;
import com.ladiesapparel.product.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductVariantRepository variantRepository;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    @Transactional
    public CartResponse addItem(AddToCartRequest request) {
        User user = authenticatedUserProvider.getCurrentUser();
        Cart cart = getOrCreateCartEntity(user);

        ProductVariant variant = variantRepository.findById(request.getVariantId())
                .orElseThrow(() -> ApiException.notFound("Product variant not found"));

        if (!variant.isActive()) {
            throw ApiException.badRequest("This product is currently unavailable");
        }

        CartItem existing = cartItemRepository
                .findByCartIdAndProductVariantId(cart.getId(), variant.getId())
                .orElse(null);

        int desiredQuantity = (existing != null ? existing.getQuantity() : 0) + request.getQuantity();

        if (desiredQuantity > variant.getStockQuantity()) {
            throw ApiException.badRequest(
                    "Only " + variant.getStockQuantity() + " item(s) left in stock for this size/color");
        }

        if (existing != null) {
            existing.setQuantity(desiredQuantity);
            cartItemRepository.save(existing);
        } else {
            CartItem item = CartItem.builder()
                    .cart(cart)
                    .productVariant(variant)
                    .quantity(request.getQuantity())
                    .build();
            cart.getItems().add(item);
            cartRepository.save(cart);
        }

        return getCart();
    }

    @Transactional
    public CartResponse updateItemQuantity(Long cartItemId, int quantity) {
        User user = authenticatedUserProvider.getCurrentUser();
        Cart cart = getOrCreateCartEntity(user);

        CartItem item = cart.getItems().stream()
                .filter(i -> i.getId().equals(cartItemId))
                .findFirst()
                .orElseThrow(() -> ApiException.notFound("Cart item not found"));

        if (quantity > item.getProductVariant().getStockQuantity()) {
            throw ApiException.badRequest(
                    "Only " + item.getProductVariant().getStockQuantity() + " item(s) left in stock");
        }

        item.setQuantity(quantity);
        cartItemRepository.save(item);

        return getCart();
    }

    @Transactional
    public CartResponse removeItem(Long cartItemId) {
        User user = authenticatedUserProvider.getCurrentUser();
        Cart cart = getOrCreateCartEntity(user);

        boolean removed = cart.getItems().removeIf(i -> i.getId().equals(cartItemId));
        if (!removed) {
            throw ApiException.notFound("Cart item not found");
        }
        cartRepository.save(cart);

        return getCart();
    }

    @Transactional
    public void clearCart() {
        User user = authenticatedUserProvider.getCurrentUser();
        Cart cart = getOrCreateCartEntity(user);
        cart.getItems().clear();
        cartRepository.save(cart);
    }

    @Transactional // ← remove readOnly
    public CartResponse getCart() {
        User user = authenticatedUserProvider.getCurrentUser();
        Cart cart = cartRepository.findByUserIdWithDetails(user.getId())
                .orElseGet(() -> createCartForUser(user)); // ← now allowed to INSERT
        return toResponse(cart);
    }

    private Cart getOrCreateCartEntity(User user) {
        return cartRepository.findByUserIdWithDetails(user.getId())
                .orElseGet(() -> createCartForUser(user));
    }

    private Cart createCartForUser(User user) {
        Cart cart = Cart.builder().user(user).build();
        return cartRepository.save(cart);
    }

    private CartResponse toResponse(Cart cart) {
        List<CartItemResponse> itemResponses = cart.getItems().stream()
                .map(this::toItemResponse)
                .collect(Collectors.toList());

        BigDecimal subtotal = itemResponses.stream()
                .map(CartItemResponse::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalMrp = itemResponses.stream()
                .map(i -> i.getMrp().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        boolean hasOutOfStock = itemResponses.stream().anyMatch(i -> !i.isInStock());
        int totalItems = itemResponses.stream().mapToInt(CartItemResponse::getQuantity).sum();

        return CartResponse.builder()
                .items(itemResponses)
                .totalItems(totalItems)
                .subtotal(subtotal)
                .totalMrp(totalMrp)
                .totalDiscount(totalMrp.subtract(subtotal))
                .hasOutOfStockItems(hasOutOfStock)
                .build();
    }

    private CartItemResponse toItemResponse(CartItem item) {
        ProductVariant variant = item.getProductVariant();
        var product = variant.getProduct();

        BigDecimal unitPrice = product.getBasePrice().add(variant.getAdditionalPrice());
        BigDecimal mrp = product.getMrp().add(variant.getAdditionalPrice());

        String imageUrl = product.getImages().stream()
                .filter(ProductImage::isPrimary)
                .findFirst()
                .or(() -> product.getImages().stream().min(Comparator.comparing(ProductImage::getDisplayOrder)))
                .map(ProductImage::getImageUrl)
                .orElse(null);

        boolean inStock = variant.isActive() && variant.getStockQuantity() >= item.getQuantity();

        return CartItemResponse.builder()
                .cartItemId(item.getId())
                .productId(product.getId())
                .productName(product.getName())
                .productSlug(product.getSlug())
                .imageUrl(imageUrl)
                .variantId(variant.getId())
                .size(variant.getSize())
                .color(variant.getColor())
                .unitPrice(unitPrice)
                .mrp(mrp)
                .quantity(item.getQuantity())
                .lineTotal(unitPrice.multiply(BigDecimal.valueOf(item.getQuantity())))
                .availableStock(variant.getStockQuantity())
                .inStock(inStock)
                .build();
    }
}