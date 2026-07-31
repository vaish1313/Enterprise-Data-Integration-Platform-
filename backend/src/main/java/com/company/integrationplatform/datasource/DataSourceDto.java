package com.company.integrationplatform.datasource;

import com.company.integrationplatform.circuitbreaker.CircuitState;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * DTO container for the Data Source Management module.
 * Entities are never exposed directly — all HTTP I/O goes through these DTOs.
 */
public class DataSourceDto {

    // ─────────────────────────────────────────────────────────────────────────
    // CREATE REQUEST
    // ─────────────────────────────────────────────────────────────────────────

    @Getter
    @Setter
    @Schema(name = "DataSourceCreateRequest", description = "Payload for registering a new data source")
    public static class CreateRequest {

        @NotBlank(message = "Name is required")
        @Size(max = 255, message = "Name must not exceed 255 characters")
        @Schema(
            description = "Unique display name for the data source",
            example = "Production PostgreSQL DB",
            requiredMode = Schema.RequiredMode.REQUIRED
        )
        private String name;

        @NotNull(message = "Source type is required")
        @Schema(
            description = "Type of the data source. Allowed values: CSV, REST_API, DATABASE",
            example = "DATABASE",
            requiredMode = Schema.RequiredMode.REQUIRED,
            allowableValues = {"CSV", "REST_API", "DATABASE"}
        )
        private DataSourceEntity.SourceType sourceType;

        @Schema(
            description = "Key-value map of connection parameters. "
                        + "For DATABASE: host, port, database, username, password. "
                        + "For REST_API: url, authType, token. "
                        + "For CSV: delimiter, encoding.",
            example = """
                {
                  "host": "db.example.com",
                  "port": "5432",
                  "database": "prod_db",
                  "username": "reader",
                  "password": "secret"
                }
                """
        )
        private Map<String, String> connectionDetails;

        @Size(max = 500, message = "Description must not exceed 500 characters")
        @Schema(
            description = "Optional human-readable description of this data source",
            example = "Main production database used for customer records"
        )
        private String description;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UPDATE REQUEST
    // ─────────────────────────────────────────────────────────────────────────

    @Getter
    @Setter
    @Schema(name = "DataSourceUpdateRequest",
            description = "Payload for updating an existing data source. All fields are optional — only provided fields are updated.")
    public static class UpdateRequest {

        @Size(max = 255, message = "Name must not exceed 255 characters")
        @Schema(
            description = "New display name for the data source",
            example = "Production PostgreSQL DB v2"
        )
        private String name;

        @Schema(
            description = "Updated connection parameters",
            example = """
                {
                  "host": "db-replica.example.com",
                  "port": "5432",
                  "database": "prod_db"
                }
                """
        )
        private Map<String, String> connectionDetails;

        @Schema(
            description = "New status for the data source. Allowed values: ACTIVE, INACTIVE, ERROR. "
                        + "DEGRADED and SUSPENDED are managed by the circuit breaker and cannot be set directly.",
            example = "ACTIVE",
            allowableValues = {"ACTIVE", "INACTIVE", "ERROR"}
        )
        private DataSourceEntity.SourceStatus status;

        @Size(max = 500, message = "Description must not exceed 500 characters")
        @Schema(
            description = "Updated description",
            example = "Switched to read replica for better performance"
        )
        private String description;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RESPONSE
    // ─────────────────────────────────────────────────────────────────────────

    @Getter
    @Builder
    @Schema(name = "DataSourceResponse", description = "Data source details returned by the API")
    public static class Response {

        @Schema(description = "Unique identifier", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        private UUID id;

        @Schema(description = "Display name", example = "Production PostgreSQL DB")
        private String name;

        @Schema(description = "Source type", example = "DATABASE")
        private DataSourceEntity.SourceType sourceType;

        @Schema(description = "Connection parameters map. Sensitive fields (password, token, secret, etc.) "
                           + "are masked as '******' in all API responses. "
                           + "Original values are stored securely in the database.")
        private Map<String, String> connectionDetails;

        @Schema(description = "Current status", example = "ACTIVE")
        private DataSourceEntity.SourceStatus status;

        @Schema(description = "Optional description", example = "Main production database")
        private String description;

        @Schema(description = "Username of the user who created this source", example = "admin")
        private String createdBy;

        @Schema(description = "Creation timestamp", example = "2026-05-23T10:15:30")
        private LocalDateTime createdAt;

        @Schema(description = "Last update timestamp", example = "2026-05-23T12:00:00")
        private LocalDateTime updatedAt;

        // ── Circuit Breaker fields ────────────────────────────────────────────

        @Schema(description = "Circuit breaker state: CLOSED (normal), OPEN (suspended), HALF_OPEN (testing)",
                example = "CLOSED")
        private CircuitState circuitState;

        @Schema(description = "Number of consecutive permanent job failures since last success", example = "0")
        private int consecutiveFailureCount;

        @Schema(description = "Timestamp of the most recent permanent job failure", example = "2026-05-23T11:00:00")
        private LocalDateTime lastFailureAt;

        @Schema(description = "Auto-recovery deadline: circuit transitions to HALF_OPEN after this timestamp",
                example = "2026-05-23T11:15:00")
        private LocalDateTime suspendedUntil;

        /**
         * Maps a {@link DataSourceEntity} to a {@link Response} DTO.
         * Entities are never returned directly from controllers.
         *
         * <p><b>Security:</b> Sensitive fields in {@code connectionDetails}
         * (password, token, secret, etc.) are replaced with {@code "******"}
         * via {@link CredentialMaskingUtil}. The original values remain in the
         * database and are never modified.
         */
        public static Response from(DataSourceEntity entity) {
            return Response.builder()
                    .id(entity.getId())
                    .name(entity.getName())
                    .sourceType(entity.getSourceType())
                    .connectionDetails(CredentialMaskingUtil.mask(entity.getConnectionDetails()))
                    .status(entity.getStatus())
                    .description(entity.getDescription())
                    .createdBy(entity.getCreatedBy())
                    .createdAt(entity.getCreatedAt())
                    .updatedAt(entity.getUpdatedAt())
                    .circuitState(entity.getCircuitState())
                    .consecutiveFailureCount(entity.getConsecutiveFailureCount())
                    .lastFailureAt(entity.getLastFailureAt())
                    .suspendedUntil(entity.getSuspendedUntil())
                    .build();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SUMMARY (lightweight list item — no connectionDetails)
    // ─────────────────────────────────────────────────────────────────────────

    @Getter
    @Builder
    @Schema(name = "DataSourceSummary",
            description = "Lightweight data source representation used in list/search results. "
                        + "Connection details are omitted for security.")
    public static class Summary {

        @Schema(description = "Unique identifier", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        private UUID id;

        @Schema(description = "Display name", example = "Production PostgreSQL DB")
        private String name;

        @Schema(description = "Source type", example = "DATABASE")
        private DataSourceEntity.SourceType sourceType;

        @Schema(description = "Current status", example = "ACTIVE")
        private DataSourceEntity.SourceStatus status;

        @Schema(description = "Optional description", example = "Main production database")
        private String description;

        @Schema(description = "Creator username", example = "admin")
        private String createdBy;

        @Schema(description = "Creation timestamp", example = "2026-05-23T10:15:30")
        private LocalDateTime createdAt;

        @Schema(description = "Circuit breaker state: CLOSED (normal), OPEN (suspended), HALF_OPEN (testing)",
                example = "CLOSED")
        private CircuitState circuitState;

        @Schema(description = "Number of consecutive permanent job failures since last success", example = "0")
        private int consecutiveFailureCount;

        /**
         * Maps a {@link DataSourceEntity} to a lightweight {@link Summary} DTO.
         * Connection details are intentionally excluded from list responses.
         */
        public static Summary from(DataSourceEntity entity) {
            return Summary.builder()
                    .id(entity.getId())
                    .name(entity.getName())
                    .sourceType(entity.getSourceType())
                    .status(entity.getStatus())
                    .description(entity.getDescription())
                    .createdBy(entity.getCreatedBy())
                    .createdAt(entity.getCreatedAt())
                    .circuitState(entity.getCircuitState())
                    .consecutiveFailureCount(entity.getConsecutiveFailureCount())
                    .build();
        }
    }
}
