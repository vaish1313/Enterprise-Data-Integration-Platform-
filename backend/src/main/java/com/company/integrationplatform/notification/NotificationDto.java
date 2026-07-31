package com.company.integrationplatform.notification;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class NotificationDto {
    private UUID id;
    private String type;
    private String title;
    private String body;          // Maps to 'message' in DB
    private LocalDateTime time;   // Maps to 'createdAt' in DB
    private boolean read;         // Maps to 'isRead' in DB
}
