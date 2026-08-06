package com.ladiesapparel.review;

import com.ladiesapparel.common.ApiResponse;
import com.ladiesapparel.common.PagedResponse;
import com.ladiesapparel.review.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    // ---------- Customer endpoints ----------

    @PostMapping("/api/reviews")
    public ResponseEntity<ApiResponse<ReviewResponse>> submit(@Valid @RequestBody ReviewRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Review submitted for moderation", reviewService.submitReview(request)));
    }

    @PostMapping(value = "/api/reviews/{id}/images", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<Object>> uploadImage(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        reviewService.uploadReviewImage(id, file);
        return ResponseEntity.ok(ApiResponse.success("Image uploaded"));
    }

    // ---------- Public endpoint ----------

    @GetMapping("/api/public/products/{productId}/reviews")
    public ResponseEntity<ApiResponse<PagedResponse<ReviewResponse>>> getApproved(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int pageSize) {

        Pageable pageable = PageRequest.of(page, pageSize, Sort.by("createdAt").descending());
        return ResponseEntity.ok(ApiResponse.success("Reviews fetched", reviewService.getApprovedReviews(productId, pageable)));
    }

    // ---------- Admin moderation ----------

    @GetMapping("/api/admin/reviews/pending")
    public ResponseEntity<ApiResponse<PagedResponse<ReviewResponse>>> getPending(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {

        Pageable pageable = PageRequest.of(page, pageSize);
        return ResponseEntity.ok(ApiResponse.success("Pending reviews fetched", reviewService.getPendingReviews(pageable)));
    }

    @PatchMapping("/api/admin/reviews/{id}/moderate")
    public ResponseEntity<ApiResponse<ReviewResponse>> moderate(@PathVariable Long id,
                                                                 @Valid @RequestBody ModerateReviewRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Review moderated", reviewService.moderate(id, request.getStatus())));
    }

    @PostMapping("/api/admin/reviews/{id}/reply")
    public ResponseEntity<ApiResponse<ReviewResponse>> reply(@PathVariable Long id,
                                                              @Valid @RequestBody AdminReplyRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Reply added", reviewService.addAdminReply(id, request.getReply())));
    }
}
