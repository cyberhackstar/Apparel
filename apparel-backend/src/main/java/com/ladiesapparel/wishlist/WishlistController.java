package com.ladiesapparel.wishlist;

import com.ladiesapparel.common.ApiResponse;
import com.ladiesapparel.wishlist.dto.WishlistItemResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/wishlist")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistService wishlistService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<WishlistItemResponse>>> getWishlist() {
        return ResponseEntity.ok(ApiResponse.success("Wishlist fetched", wishlistService.getWishlist()));
    }

    @PostMapping("/{productId}")
    public ResponseEntity<ApiResponse<Object>> addToWishlist(@PathVariable Long productId) {
        wishlistService.addToWishlist(productId);
        return ResponseEntity.ok(ApiResponse.success("Added to wishlist"));
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<ApiResponse<Object>> removeFromWishlist(@PathVariable Long productId) {
        wishlistService.removeFromWishlist(productId);
        return ResponseEntity.ok(ApiResponse.success("Removed from wishlist"));
    }
}
