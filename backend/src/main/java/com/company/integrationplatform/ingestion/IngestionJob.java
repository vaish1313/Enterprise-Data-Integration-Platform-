package com.company.integrationplatform.ingestion;

import com.company.integrationplatform.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a single ingestion job execution.
 * Tracks lifecycle, file metadata, record counts, and final status.
 */
@Entity
@Table(name = "ingestion_jobs",
        indexes = {
                @Index(name = "idx_ingest_job_datasource", columnList = "data_source_id"),
                @Index(name = "idx_ingest_job_status",     columnList = "status"),
                @Index(name = "idx_ingest_job_type",       columnList = "ingestion_type")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IngestionJob extends BaseEntity {

    @Column(name = "data_source_id", nullable = false)
    private UUID dataSourceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private JobStatus status = JobStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "ingestion_type", nullable = false, length = 50)
    private IngestionType ingestionType;

    /** Original filename of the uploaded CSV (null for API/scheduled jobs). */
    @Column(name = "file_name", length = 500)
    private String fileName;

    /** Total rows found in the CSV (header excluded). Set after parsing. */
    @Column(name = "total_records")
    @Builder.Default
    private int totalRecords = 0;

    @Column(name = "records_processed")
    @Builder.Default
    private int recordsProcessed = 0;

    @Column(name = "records_failed")
    @Builder.Default
    private int recordsFailed = 0;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "triggered_by", length = 100)
    private String triggeredBy;

    public enum JobStatus {
        PENDING, RUNNING, COMPLETED, FAILED, PARTIAL
    }

    public enum IngestionType {
        CSV, REST_API, SCHEDULED
    }
}
