package com.ladiesapparel.product.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkUploadResult {
    private int totalRowsRead;
    private int productsCreated;
    private int productsFailed;
    private List<String> errors;
}
