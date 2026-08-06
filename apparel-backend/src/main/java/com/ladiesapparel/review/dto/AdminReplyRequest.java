package com.ladiesapparel.review.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminReplyRequest {

    @NotBlank(message = "Reply is required")
    @Size(max = 500)
    private String reply;
}
