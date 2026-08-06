package com.ladiesapparel.review.dto;

import com.ladiesapparel.review.ReviewStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ModerateReviewRequest {

    @NotNull(message = "Status is required")
    private ReviewStatus status; // APPROVED or REJECTED
}
