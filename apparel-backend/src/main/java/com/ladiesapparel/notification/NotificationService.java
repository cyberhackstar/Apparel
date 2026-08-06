package com.ladiesapparel.notification;

import com.ladiesapparel.auth.AuthenticatedUserProvider;
import com.ladiesapparel.auth.User;
import com.ladiesapparel.common.ApiException;
import com.ladiesapparel.common.PagedResponse;
import com.ladiesapparel.notification.dto.NotificationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    /** Internal use by other services (order status changes etc) — not exposed via a controller. */
    @Transactional
    public void create(User user, String title, String message, NotificationType type, String link) {
        Notification notification = Notification.builder()
                .user(user)
                .title(title)
                .message(message)
                .type(type)
                .link(link)
                .read(false)
                .build();
        notificationRepository.save(notification);
    }

    public PagedResponse<NotificationResponse> getMyNotifications(Pageable pageable) {
        User user = authenticatedUserProvider.getCurrentUser();
        Page<Notification> page = notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), pageable);
        return PagedResponse.from(page.map(this::toResponse));
    }

    public long getUnreadCount() {
        User user = authenticatedUserProvider.getCurrentUser();
        return notificationRepository.countByUserIdAndReadFalse(user.getId());
    }

    @Transactional
    public void markAsRead(Long id) {
        User user = authenticatedUserProvider.getCurrentUser();
        Notification notification = notificationRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> ApiException.notFound("Notification not found"));
        notification.setRead(true);
        notificationRepository.save(notification);
    }

    @Transactional
    public void markAllAsRead() {
        User user = authenticatedUserProvider.getCurrentUser();
        notificationRepository.markAllAsRead(user.getId());
    }

    private NotificationResponse toResponse(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .title(n.getTitle())
                .message(n.getMessage())
                .type(n.getType())
                .link(n.getLink())
                .read(n.isRead())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
