package com.ladiesapparel.banner;

import com.ladiesapparel.common.ApiException;
import com.ladiesapparel.media.CloudinaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BannerService {

    private final BannerRepository bannerRepository;
    private final CloudinaryService cloudinaryService;

    @Transactional
    @CacheEvict(cacheNames = "activeBanners", allEntries = true)
    public BannerResponse createBanner(String title, String linkUrl, Integer displayOrder, MultipartFile file) {
        var upload = cloudinaryService.upload(file, "banners");

        Banner banner = Banner.builder()
                .title(title)
                .imageUrl(upload.url())
                .publicId(upload.publicId())
                .linkUrl(linkUrl)
                .displayOrder(displayOrder != null ? displayOrder : 0)
                .active(true)
                .build();

        bannerRepository.save(banner);
        return toResponse(banner);
    }

    @Transactional
    @CacheEvict(cacheNames = "activeBanners", allEntries = true)
    public BannerResponse updateBanner(Long id, String title, String linkUrl, Integer displayOrder, boolean active) {
        Banner banner = bannerRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Banner not found"));

        banner.setTitle(title);
        banner.setLinkUrl(linkUrl);
        if (displayOrder != null) {
            banner.setDisplayOrder(displayOrder);
        }
        banner.setActive(active);

        bannerRepository.save(banner);
        return toResponse(banner);
    }

    @Transactional
    @CacheEvict(cacheNames = "activeBanners", allEntries = true)
    public void deleteBanner(Long id) {
        Banner banner = bannerRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Banner not found"));
        cloudinaryService.delete(banner.getPublicId());
        bannerRepository.delete(banner);
    }

    @Cacheable(cacheNames = "activeBanners")
    public List<BannerResponse> getActiveBanners() {
        return bannerRepository.findByActiveTrueOrderByDisplayOrderAsc()
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<BannerResponse> getAllForAdmin() {
        return bannerRepository.findAllByOrderByDisplayOrderAsc()
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    private BannerResponse toResponse(Banner banner) {
        return BannerResponse.builder()
                .id(banner.getId())
                .title(banner.getTitle())
                .imageUrl(banner.getImageUrl())
                .linkUrl(banner.getLinkUrl())
                .displayOrder(banner.getDisplayOrder())
                .active(banner.isActive())
                .build();
    }
}
