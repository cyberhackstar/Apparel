package com.ladiesapparel.review;

import com.ladiesapparel.auth.AuthenticatedUserProvider;
import com.ladiesapparel.auth.User;
import com.ladiesapparel.common.ApiException;
import com.ladiesapparel.common.PagedResponse;
import com.ladiesapparel.media.CloudinaryService;
import com.ladiesapparel.order.OrderItemRepository;
import com.ladiesapparel.product.Product;
import com.ladiesapparel.product.ProductRepository;
import com.ladiesapparel.review.dto.ReviewRequest;
import com.ladiesapparel.review.dto.ReviewResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ReviewImageRepository reviewImageRepository;
    private final ProductRepository productRepository;
    private final OrderItemRepository orderItemRepository;
    private final CloudinaryService cloudinaryService;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    @Transactional
    public ReviewResponse submitReview(ReviewRequest request) {
        User user = authenticatedUserProvider.getCurrentUser();

        if (reviewRepository.existsByProductIdAndUserId(request.getProductId(), user.getId())) {
            throw ApiException.conflict("You have already reviewed this product");
        }

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> ApiException.notFound("Product not found"));

        boolean verifiedPurchase = orderItemRepository.existsDeliveredPurchase(product.getId(), user.getId());

        Review review = Review.builder()
                .product(product)
                .user(user)
                .rating(request.getRating())
                .comment(request.getComment())
                .verifiedPurchase(verifiedPurchase)
                .status(ReviewStatus.PENDING) // goes live only after admin moderation
                .build();

        reviewRepository.save(review);
        return toResponse(review);
    }

    @Transactional
    public void uploadReviewImage(Long reviewId, MultipartFile file) {
        User user = authenticatedUserProvider.getCurrentUser();
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> ApiException.notFound("Review not found"));

        if (!review.getUser().getId().equals(user.getId())) {
            throw ApiException.unauthorized("You can only add images to your own review");
        }

        var result = cloudinaryService.upload(file, "reviews/" + reviewId);

        ReviewImage image = ReviewImage.builder()
                .review(review)
                .imageUrl(result.url())
                .publicId(result.publicId())
                .build();

        review.getImages().add(image);
        reviewRepository.save(review);
    }

    public PagedResponse<ReviewResponse> getApprovedReviews(Long productId, Pageable pageable) {
        Page<Review> page = reviewRepository.findByProductIdAndStatusOrderByCreatedAtDesc(
                productId, ReviewStatus.APPROVED, pageable);
        return PagedResponse.from(page.map(this::toResponse));
    }

    // ---------- Admin moderation ----------

    public PagedResponse<ReviewResponse> getPendingReviews(Pageable pageable) {
        Page<Review> page = reviewRepository.findByStatusOrderByCreatedAtAsc(ReviewStatus.PENDING, pageable);
        return PagedResponse.from(page.map(this::toResponse));
    }

    @Transactional
    public ReviewResponse moderate(Long reviewId, ReviewStatus newStatus) {
        if (newStatus != ReviewStatus.APPROVED && newStatus != ReviewStatus.REJECTED) {
            throw ApiException.badRequest("Status must be APPROVED or REJECTED");
        }

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> ApiException.notFound("Review not found"));

        review.setStatus(newStatus);
        reviewRepository.save(review);

        recalculateProductRating(review.getProduct().getId());
        return toResponse(review);
    }

    @Transactional
    public ReviewResponse addAdminReply(Long reviewId, String reply) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> ApiException.notFound("Review not found"));
        review.setAdminReply(reply);
        reviewRepository.save(review);
        return toResponse(review);
    }

    private void recalculateProductRating(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> ApiException.notFound("Product not found"));

        List<Review> approved = reviewRepository.findByProductIdAndStatus(productId, ReviewStatus.APPROVED);

        if (approved.isEmpty()) {
            product.setAverageRating(0.0);
            product.setRatingCount(0);
        } else {
            double avg = approved.stream().mapToInt(Review::getRating).average().orElse(0.0);
            product.setAverageRating(Math.round(avg * 10.0) / 10.0); // one decimal place
            product.setRatingCount(approved.size());
        }

        productRepository.save(product);
    }

    private ReviewResponse toResponse(Review review) {
        List<String> imageUrls = review.getImages().stream()
                .map(ReviewImage::getImageUrl)
                .collect(Collectors.toList());

        return ReviewResponse.builder()
                .id(review.getId())
                .productId(review.getProduct().getId())
                .productName(review.getProduct().getName())
                .userFullName(review.getUser().getFullName())
                .rating(review.getRating())
                .comment(review.getComment())
                .verifiedPurchase(review.isVerifiedPurchase())
                .status(review.getStatus())
                .adminReply(review.getAdminReply())
                .imageUrls(imageUrls)
                .createdAt(review.getCreatedAt())
                .build();
    }
}
