package com.ladiesapparel.product;

import com.ladiesapparel.common.ApiException;
import com.ladiesapparel.product.dto.BulkUploadResult;
import com.ladiesapparel.product.dto.ProductRequest;
import com.ladiesapparel.product.dto.ProductVariantRequest;
import com.opencsv.CSVReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Parses an admin-uploaded CSV for bulk product creation.
 * Expected columns (one row per size/color variant — rows sharing the same
 * productName + categoryId are grouped into a single product with multiple variants):
 * <p>
 * productName,description,categoryId,brand,fabric,basePrice,mrp,discountPercentage,
 * gstPercentage,tags,size,color,sku,stockQuantity,additionalPrice
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BulkUploadService {

    private final ProductService productService;
    private static final int EXPECTED_COLUMNS = 15;

    public BulkUploadResult uploadCsv(MultipartFile file) {
        List<String[]> rows;
        try (CSVReader reader = new CSVReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            rows = reader.readAll();
        } catch (Exception e) {
            throw ApiException.badRequest("Could not read CSV file: " + e.getMessage());
        }

        if (rows.isEmpty()) {
            throw ApiException.badRequest("CSV file is empty");
        }

        // skip header row
        List<String[]> dataRows = rows.subList(1, rows.size());

        // group variant rows by product (name + category)
        LinkedHashMap<String, ProductRequest> productsByKey = new LinkedHashMap<>();
        List<String> errors = new ArrayList<>();
        int rowNumber = 1; // header was row 0

        for (String[] row : dataRows) {
            rowNumber++;
            if (row.length < EXPECTED_COLUMNS) {
                errors.add("Row " + rowNumber + ": expected " + EXPECTED_COLUMNS + " columns, found " + row.length + " — skipped");
                continue;
            }

            try {
                String productName = row[0].trim();
                Long categoryId = Long.parseLong(row[2].trim());
                String key = productName.toLowerCase() + "::" + categoryId;

                ProductRequest productRequest = productsByKey.computeIfAbsent(key, k -> {
                    ProductRequest pr = new ProductRequest();
                    pr.setName(productName);
                    pr.setDescription(row[1].trim());
                    pr.setCategoryId(categoryId);
                    pr.setBrand(row[3].trim());
                    pr.setFabric(row[4].trim());
                    pr.setBasePrice(new BigDecimal(row[5].trim()));
                    pr.setMrp(new BigDecimal(row[6].trim()));
                    pr.setDiscountPercentage(row[7].isBlank() ? BigDecimal.ZERO : new BigDecimal(row[7].trim()));
                    pr.setGstPercentage(row[8].isBlank() ? new BigDecimal("5.00") : new BigDecimal(row[8].trim()));
                    pr.setTags(row[9].trim());
                    pr.setVariants(new ArrayList<>());
                    return pr;
                });

                ProductVariantRequest variant = new ProductVariantRequest();
                variant.setSize(row[10].trim());
                variant.setColor(row[11].trim());
                variant.setSku(row[12].trim());
                variant.setStockQuantity(Integer.parseInt(row[13].trim()));
                variant.setAdditionalPrice(row[14].isBlank() ? BigDecimal.ZERO : new BigDecimal(row[14].trim()));
                productRequest.getVariants().add(variant);

            } catch (Exception e) {
                errors.add("Row " + rowNumber + ": " + e.getMessage() + " — skipped");
            }
        }

        int created = 0;
        int failed = 0;
        for (ProductRequest request : productsByKey.values()) {
            try {
                productService.createProduct(request);
                created++;
            } catch (Exception e) {
                failed++;
                errors.add("Product '" + request.getName() + "': " + e.getMessage());
                log.warn("Bulk upload failed for product '{}': {}", request.getName(), e.getMessage());
            }
        }

        return BulkUploadResult.builder()
                .totalRowsRead(dataRows.size())
                .productsCreated(created)
                .productsFailed(failed)
                .errors(errors)
                .build();
    }
}
