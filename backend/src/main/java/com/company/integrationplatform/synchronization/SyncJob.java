package com.company.integrationplatform.synchronization;

import com.company.integrationplatform.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a single synchronization job execution.
 *
 * <p>A sync job fetches all PROCESSED, un-synchronized ingestion records
 * for a data source, validates them, marks them synchronized, and
 * generates a completion report.
 *
 * <p><b>Status lifecycle:</b>
 * {@code PENDING} → {@code RUNNING} → {@code COMPLETED} | {@code FAILED}
 */
@Entity
@Table(name = "sync_jobs",
        indexes = {
                @Index(name = "idx_sync_job_datasource", columnList = "data_source_id"),
                @Index(name = "idx_sync_job_status",     columnList = "status"),
                @Index(name = "idx_sync_job_started",    columnList = "started_at")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SyncJob extends BaseEntity {

    /** The data source whose records this job synchronizes. */
    @Column(name = "data_source_id", nullable = false)
    private UUID dataSourceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private SyncStatus status = SyncStatus.PENDING;

    /** Number of records successfully synchronized. */
    @Column(name = "records_processed")
    @Builder.Default
    private long recordsProcessed = 0L;

    /** Number of records that failed validation or sync. */
    @Column(name = "records_failed")
    @Builder.Default
    private long recordsFailed = 0L;

    /** Number of records skipped (already synchronized or no transformed data). */
    @Column(name = "records_skipped")
    @Builder.Default
    private long recordsSkipped = 0L;

    /** Total records considered for this sync run. */
    @Column(name = "total_records")
    @Builder.Default
    private long totalRecords = 0L;

    /** Job start timestamp. */
    @Column(name = "started_at")
    private LocalDateTime startedAt;

    /** Job completion timestamp. */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    /**
     * Total execution time in milliseconds.
     * Computed as {@code completedAt - startedAt} when the job finishes.
     */
    @Column(name = "execution_time_ms")
    private Long executionTimeMs;

    /** Error message if the job failed entirely. */
    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    /** Who triggered this job: "SCHEDULER" or a username. */
    @Column(name = "triggered_by", length = 100)
    @Builder.Default
    private String triggeredBy = "SCHEDULER";

    /** Validation summary — counts of records that passed/failed validation. */
    @Column(name = "validation_passed")
    @Builder.Default
    private long validationPassed = 0L;

    @Column(name = "validation_failed")
    @Builder.Default
    private long validationFailed = 0L;

    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private int retryCount = 0;

    @Column(name = "last_attempted_at")
    private LocalDateTime lastAttemptedAt;

    public enum SyncStatus {
        PENDING, RUNNING, RETRYING, COMPLETED, FAILED
    }
}
