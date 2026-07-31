package com.company.integrationplatform.synchronization;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO container for the Synchronization Engine module.
 */
public class SyncDto {

    // ─────────────────────────────────────────────────────────────────────────
    // RUN REQUEST
    // ─────────────────────────────────────────────────────────────────────────

    @Getter
    @Setter
    @Schema(name = "SyncRunRequest",
            description = "Request body for triggering a manual synchronization run")
    public static class RunRequest {

        @NotNull(message = "dataSourceId is required")
        @Schema(
            description = "UUID of the data source to synchronize",
            example = "7c9e6679-7425-40de-944b-e07fc1f90ae7",
            requiredMode = Schema.RequiredMode.REQUIRED
        )
        private UUID dataSourceId;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // JOB RESPONSE
    // ─────────────────────────────────────────────────────────────────────────

    @Getter
    @Builder
    @Schema(name = "SyncJobResponse", description = "Full details of a synchronization job")
    public static class JobResponse {

        @Schema(description = "Sync job UUID",
                example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        private UUID id;

        @Schema(description = "Data source UUID this job synchronized",
                example = "7c9e6679-7425-40de-944b-e07fc1f90ae7")
        private UUID dataSourceId;

        @Schema(description = "Job status",
                example = "COMPLETED",
                allowableValues = {"PENDING", "RUNNING", "COMPLETED", "FAILED"})
        private SyncJob.SyncStatus status;

        @Schema(description = "Total records considered for this sync run", example = "1500")
        private long totalRecords;

        @Schema(description = "Records that passed validation", example = "1498")
        private long validationPassed;

        @Schema(description = "Records that failed validation", example = "2")
        private long validationFailed;

        @Schema(description = "Records successfully synchronized", example = "1498")
        private long recordsProcessed;

        @Schema(description = "Records that failed synchronization", example = "0")
        private long recordsFailed;

        @Schema(description = "Records skipped (already synced or no transformed data)", example = "0")
        private long recordsSkipped;

        @Schema(description = "Job start timestamp", example = "2026-05-23T10:15:30")
        private LocalDateTime startedAt;

        @Schema(description = "Job completion timestamp", example = "2026-05-23T10:15:45")
        private LocalDateTime completedAt;

        @Schema(description = "Total execution time in milliseconds", example = "15234")
        private Long executionTimeMs;

        @Schema(description = "Error message if the job failed entirely")
        private String errorMessage;

        @Schema(description = "Who triggered this job: SCHEDULER or a username",
                example = "SCHEDULER")
        private String triggeredBy;

        @Schema(description = "Job creation timestamp", example = "2026-05-23T10:15:29")
        private LocalDateTime createdAt;

        public static JobResponse from(SyncJob job) {
            return JobResponse.builder()
                    .id(job.getId())
                    .dataSourceId(job.getDataSourceId())
                    .status(job.getStatus())
                    .totalRecords(job.getTotalRecords())
                    .validationPassed(job.getValidationPassed())
                    .validationFailed(job.getValidationFailed())
                    .recordsProcessed(job.getRecordsProcessed())
                    .recordsFailed(job.getRecordsFailed())
                    .recordsSkipped(job.getRecordsSkipped())
                    .startedAt(job.getStartedAt())
                    .completedAt(job.getCompletedAt())
                    .executionTimeMs(job.getExecutionTimeMs())
                    .errorMessage(job.getErrorMessage())
                    .triggeredBy(job.getTriggeredBy())
                    .createdAt(job.getCreatedAt())
                    .build();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // STATISTICS RESPONSE
    // ─────────────────────────────────────────────────────────────────────────

    @Getter
    @Builder
    @Schema(name = "SyncStatisticsResponse",
            description = "Platform-wide synchronization statistics")
    public static class StatisticsResponse {

        @Schema(description = "Total sync jobs ever run", example = "342")
        private long totalJobs;

        @Schema(description = "Jobs with status COMPLETED", example = "330")
        private long completedJobs;

        @Schema(description = "Jobs with status FAILED", example = "8")
        private long failedJobs;

        @Schema(description = "Jobs currently RUNNING", example = "1")
        private long runningJobs;

        @Schema(description = "Jobs with status PENDING", example = "3")
        private long pendingJobs;

        @Schema(description = "Total records synchronized across all completed jobs",
                example = "450000")
        private long totalRecordsSynchronized;

        @Schema(description = "Total records that failed across all jobs", example = "120")
        private long totalRecordsFailed;

        @Schema(description = "Total records pending synchronization right now",
                example = "250")
        private long recordsPendingSync;

        @Schema(description = "Total records already synchronized", example = "449750")
        private long recordsAlreadySynchronized;

        @Schema(description = "Average job execution time in milliseconds", example = "12450")
        private long avgExecutionTimeMs;

        @Schema(description = "Maximum job execution time in milliseconds", example = "45000")
        private long maxExecutionTimeMs;

        @Schema(description = "Timestamp of the last successful sync",
                example = "2026-05-23T10:15:45")
        private LocalDateTime lastSuccessfulSyncAt;

        @Schema(description = "Success rate as a percentage (0-100)", example = "96.5")
        private double successRatePercent;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SYNC REPORT (returned with each job completion)
    // ─────────────────────────────────────────────────────────────────────────

    @Getter
    @Builder
    @Schema(name = "SyncReport",
            description = "Detailed report generated at the end of a sync job")
    public static class SyncReport {

        @Schema(description = "The completed sync job")
        private JobResponse job;

        @Schema(description = "Human-readable summary of the sync run",
                example = "Sync completed: 1498 records synchronized, 2 failed validation, 0 skipped.")
        private String summary;

        @Schema(description = "Whether the sync was fully successful", example = "true")
        private boolean fullySuccessful;

        @Schema(description = "Recommended action if the sync was not fully successful",
                example = "Review failed records and re-run transformation before next sync.")
        private String recommendation;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // LEGACY RESPONSE ALIAS (kept for backward compatibility with SyncExecutor)
    // ─────────────────────────────────────────────────────────────────────────

    @Getter
    @Builder
    public static class Response {

        private UUID id;
        private UUID dataSourceId;
        private SyncJob.SyncStatus status;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private long recordsProcessed;
        private long recordsFailed;
        private String errorMessage;
        private String triggeredBy;
        private LocalDateTime createdAt;

        public static Response from(SyncJob job) {
            return Response.builder()
                    .id(job.getId())
                    .dataSourceId(job.getDataSourceId())
                    .status(job.getStatus())
                    .startTime(job.getStartedAt())
                    .endTime(job.getCompletedAt())
                    .recordsProcessed(job.getRecordsProcessed())
                    .recordsFailed(job.getRecordsFailed())
                    .errorMessage(job.getErrorMessage())
                    .triggeredBy(job.getTriggeredBy())
                    .createdAt(job.getCreatedAt())
                    .build();
        }
    }
}
