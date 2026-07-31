package com.company.integrationplatform.ingestion;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * DTO container for the CSV Ingestion module.
 * Entities are never returned directly — all HTTP responses use these DTOs.
 */
public class IngestionDto {

    // ─────────────────────────────────────────────────────────────────────────
    // JOB RESPONSE — returned for every job query
    // ─────────────────────────────────────────────────────────────────────────

    @Getter
    @Builder
    @Schema(name = "IngestionJobResponse", description = "Full details of a CSV ingestion job")
    public static class JobResponse {

        @Schema(description = "Unique job identifier", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        private UUID id;

        @Schema(description = "Associated data source ID", example = "7c9e6679-7425-40de-944b-e07fc1f90ae7")
        private UUID dataSourceId;

        @Schema(description = "Current job status", example = "COMPLETED",
                allowableValues = {"PENDING", "RUNNING", "COMPLETED", "FAILED", "PARTIAL"})
        private IngestionJob.JobStatus status;

        @Schema(description = "Type of ingestion", example = "CSV",
                allowableValues = {"CSV", "REST_API", "SCHEDULED"})
        private IngestionJob.IngestionType ingestionType;

        @Schema(description = "Original filename of the uploaded CSV", example = "sales_data_may2026.csv")
        private String fileName;

        @Schema(description = "Total data rows in the CSV (header excluded)", example = "1500")
        private long totalRecords;

        @Schema(description = "Number of rows successfully processed", example = "1498")
        private long recordsProcessed;

        @Schema(description = "Number of rows that failed processing", example = "2")
        private long recordsFailed;

        @Schema(description = "Job start timestamp", example = "2026-05-23T10:15:30")
        private LocalDateTime startedAt;

        @Schema(description = "Job completion timestamp", example = "2026-05-23T10:15:45")
        private LocalDateTime completedAt;

        @Schema(description = "Error message if the job failed entirely", example = "CSV file is empty")
        private String errorMessage;

        @Schema(description = "Username who triggered the job", example = "analyst_user")
        private String triggeredBy;

        @Schema(description = "Job creation timestamp", example = "2026-05-23T10:15:29")
        private LocalDateTime createdAt;

        public static JobResponse from(IngestionJob job) {
            return JobResponse.builder()
                    .id(job.getId())
                    .dataSourceId(job.getDataSourceId())
                    .status(job.getStatus())
                    .ingestionType(job.getIngestionType())
                    .fileName(job.getFileName())
                    .totalRecords(job.getTotalRecords())
                    .recordsProcessed(job.getRecordsProcessed())
                    .recordsFailed(job.getRecordsFailed())
                    .startedAt(job.getStartedAt())
                    .completedAt(job.getCompletedAt())
                    .errorMessage(job.getErrorMessage())
                    .triggeredBy(job.getTriggeredBy())
                    .createdAt(job.getCreatedAt())
                    .build();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // JOB DETAIL RESPONSE — job + failed row details
    // ─────────────────────────────────────────────────────────────────────────

    @Getter
    @Builder
    @Schema(name = "IngestionJobDetailResponse",
            description = "Full job details including a summary of failed rows")
    public static class JobDetailResponse {

        @Schema(description = "Job summary")
        private JobResponse job;

        @Schema(description = "List of rows that failed processing (up to 100 shown)")
        private List<FailedRowResponse> failedRows;

        @Schema(description = "Total number of failed rows", example = "2")
        private long totalFailedRows;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FAILED ROW RESPONSE
    // ─────────────────────────────────────────────────────────────────────────

    @Getter
    @Builder
    @Schema(name = "FailedRowResponse", description = "Details of a single row that failed during ingestion")
    public static class FailedRowResponse {

        @Schema(description = "1-based row number in the source CSV", example = "42")
        private Integer rowNumber;

        @Schema(description = "Column that caused the failure (if applicable)", example = "email")
        private String columnName;

        @Schema(description = "Error description", example = "Value 'not-an-email' is not a valid email address")
        private String errorMessage;

        @Schema(description = "Raw data from the failed row")
        private Map<String, Object> rawData;

        public static FailedRowResponse from(IngestionRecord record) {
            return FailedRowResponse.builder()
                    .rowNumber(record.getSourceRowNumber())
                    .columnName(record.getColumnName())
                    .errorMessage(record.getErrorMessage())
                    .rawData(record.getRawData())
                    .build();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // API INGESTION REQUEST (kept for REST_API ingestion)
    // ─────────────────────────────────────────────────────────────────────────

    @Getter
    @Builder
    @Schema(name = "ApiIngestionRequest", description = "Request body for triggering REST API ingestion")
    public static class ApiIngestionRequest {

        @Schema(description = "Data source ID to ingest from",
                example = "7c9e6679-7425-40de-944b-e07fc1f90ae7")
        private UUID dataSourceId;

        @Schema(description = "Optional URL override (uses data source URL if omitted)",
                example = "https://api.example.com/v2/data")
        private String endpointOverride;
    }
}
