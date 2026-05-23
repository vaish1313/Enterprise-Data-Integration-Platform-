package com.company.integrationplatform.audit;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class AuditDto {

    private UUID id;
    private String action;
    private String username;
    private String status;
    private String details;
    private String ipAddress;
    private LocalDateTime timestamp;

    public static AuditDto from(AuditEntity entity) {
        return AuditDto.builder()
                .id(entity.getId())
                .action(entity.getAction())
                .username(entity.getUsername())
                .status(entity.getStatus())
                .details(entity.getDetails())
                .ipAddress(entity.getIpAddress())
                .timestamp(entity.getTimestamp())
                .build();
    }
}
