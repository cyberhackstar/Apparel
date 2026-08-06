package com.ladiesapparel.cart;

import com.ladiesapparel.cart.dto.AddToCartRequest;
import com.ladiesapparel.cart.dto.CartResponse;
import com.ladiesapparel.cart.dto.UpdateCartItemRequest;
import com.ladiesapparel.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping
    public ResponseEntity<ApiResponse<CartResponse>> getCart() {
        return ResponseEntity.ok(ApiResponse.success("Cart fetched", cartService.getCart()));
    }

    @PostMapping("/items")
    public ResponseEntity<ApiResponse<CartResponse>> addItem(@Valid @RequestBody AddToCartRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Item added to cart", cartService.addItem(request)));
    }

    @PutMapping("/items/{cartItemId}")
    public ResponseEntity<ApiResponse<CartResponse>> updateItem(@PathVariable Long cartItemId,
                                                                 @Valid @RequestBody UpdateCartItemRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Cart updated",
                cartService.updateItemQuantity(cartItemId, request.getQuantity())));
    }

    @DeleteMapping("/items/{cartItemId}")
    public ResponseEntity<ApiResponse<CartResponse>> removeItem(@PathVariable Long cartItemId) {
        return ResponseEntity.ok(ApiResponse.success("Item removed from cart", cartService.removeItem(cartItemId)));
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<Object>> clearCart() {
        cartService.clearCart();
        return ResponseEntity.ok(ApiResponse.success("Cart cleared"));
    }
}
