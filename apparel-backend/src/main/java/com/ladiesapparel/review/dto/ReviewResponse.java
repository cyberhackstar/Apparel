package com.ladiesapparel.review.dto;

import com.ladiesapparel.review.ReviewStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponse {
    private Long id;
    private Long productId;
    private String productName;
    private String userFullName;
    private Integer rating;
    private String comment;
    private boolean verifiedPurchase;
    private ReviewStatus status;
    private String adminReply;
    private List<String> imageUrls;
    private Instant createdAt;
}
