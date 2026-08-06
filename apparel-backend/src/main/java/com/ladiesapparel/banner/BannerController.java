package com.ladiesapparel.banner;

import com.ladiesapparel.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class BannerController {

    private final BannerService bannerService;

    // ---------- Admin endpoints ----------

    @PostMapping(value = "/api/admin/banners", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<BannerResponse>> create(
            @RequestParam String title,
            @RequestParam(required = false) String linkUrl,
            @RequestParam(required = false) Integer displayOrder,
            @RequestParam("file") MultipartFile file) {

        return ResponseEntity.ok(ApiResponse.success("Banner created",
                bannerService.createBanner(title, linkUrl, displayOrder, file)));
    }

    @PutMapping("/api/admin/banners/{id}")
    public ResponseEntity<ApiResponse<BannerResponse>> update(
            @PathVariable Long id,
            @RequestParam String title,
            @RequestParam(required = false) String linkUrl,
            @RequestParam(required = false) Integer displayOrder,
            @RequestParam(defaultValue = "true") boolean active) {

        return ResponseEntity.ok(ApiResponse.success("Banner updated",
                bannerService.updateBanner(id, title, linkUrl, displayOrder, active)));
    }

    @DeleteMapping("/api/admin/banners/{id}")
    public ResponseEntity<ApiResponse<Object>> delete(@PathVariable Long id) {
        bannerService.deleteBanner(id);
        return ResponseEntity.ok(ApiResponse.success("Banner deleted"));
    }

    @GetMapping("/api/admin/banners")
    public ResponseEntity<ApiResponse<List<BannerResponse>>> listAllForAdmin() {
        return ResponseEntity.ok(ApiResponse.success("Banners fetched", bannerService.getAllForAdmin()));
    }

    // ---------- Public endpoint ----------

    @GetMapping("/api/public/banners")
    public ResponseEntity<ApiResponse<List<BannerResponse>>> listActive() {
        return ResponseEntity.ok(ApiResponse.success("Banners fetched", bannerService.getActiveBanners()));
    }
}
