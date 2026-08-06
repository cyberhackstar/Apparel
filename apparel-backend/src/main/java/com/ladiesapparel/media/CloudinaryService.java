package com.ladiesapparel.media;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.ladiesapparel.common.ApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CloudinaryService {

    private final Cloudinary cloudinary;

    private static final long MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024; // 5MB
    private static final String FOLDER = "ladies-apparel";

    public UploadResult upload(MultipartFile file, String subFolder) {
        validateFile(file);

        try {
            Map params = ObjectUtils.asMap(
                    "folder", FOLDER + "/" + subFolder,
                    "public_id", UUID.randomUUID().toString(),
                    "overwrite", true,
                    "resource_type", "image"
            );

            Map uploadResult = cloudinary.uploader().upload(file.getBytes(), params);

            String url = (String) uploadResult.get("secure_url");
            String publicId = (String) uploadResult.get("public_id");

            return new UploadResult(url, publicId);
        } catch (IOException e) {
            log.error("Cloudinary upload failed: {}", e.getMessage());
            throw ApiException.badRequest("Image upload failed. Please try again.");
        }
    }

    public void delete(String publicId) {
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
        } catch (IOException e) {
            log.error("Cloudinary delete failed for {}: {}", publicId, e.getMessage());
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw ApiException.badRequest("Image file is required");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw ApiException.badRequest("Image size must not exceed 5MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !(contentType.equals("image/jpeg")
                || contentType.equals("image/png")
                || contentType.equals("image/webp"))) {
            throw ApiException.badRequest("Only JPEG, PNG, or WEBP images are allowed");
        }
    }

    public record UploadResult(String url, String publicId) {
    }
}
