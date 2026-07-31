package com.company.integrationplatform.notification;

import com.company.integrationplatform.exception.ResourceNotFoundException;
import com.company.integrationplatform.user.entity.User;
import com.company.integrationplatform.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Page<NotificationDto> getUserNotifications(String username, Pageable pageable) {
        User user = getUser(username);
        return notificationRepository.findAllByUserId(user.getId(), pageable)
                .map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(String username) {
        User user = getUser(username);
        return notificationRepository.countByUserIdAndIsReadFalse(user.getId());
    }

    @Transactional
    public NotificationDto markAsRead(String username, UUID notificationId) {
        NotificationEntity notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        
        // Ensure user owns this notification
        if (!notification.getUser().getUsername().equals(username)) {
            throw new ResourceNotFoundException("Notification not found");
        }

        notification.setRead(true);
        return mapToDto(notificationRepository.save(notification));
    }

    @Transactional
    public void markAllAsRead(String username) {
        User user = getUser(username);
        notificationRepository.markAllAsReadByUserId(user.getId());
    }

    @Transactional
    public void deleteNotification(String username, UUID notificationId) {
        NotificationEntity notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));

        if (!notification.getUser().getUsername().equals(username)) {
            throw new ResourceNotFoundException("Notification not found");
        }

        notificationRepository.delete(notification);
    }

    @Transactional
    public void deleteAll(String username) {
        User user = getUser(username);
        notificationRepository.deleteAllByUserId(user.getId());
    }

    // --- Internal hook for system events (Audit style) ---
    @Transactional
    public void createSystemNotification(String username, String type, String title, String message, String entityType, UUID entityId) {
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) return;

        NotificationEntity notification = NotificationEntity.builder()
                .user(user)
                .type(type.toLowerCase()) // success, error, warning, info
                .title(title)
                .message(message)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .relatedEntityType(entityType)
                .relatedEntityId(entityId)
                .build();

        notificationRepository.save(notification);
    }

    private User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private NotificationDto mapToDto(NotificationEntity entity) {
        return NotificationDto.builder()
                .id(entity.getId())
                .type(entity.getType())
                .title(entity.getTitle())
                .body(entity.getMessage())
                .time(entity.getCreatedAt())
                .read(entity.isRead())
                .build();
    }
}
