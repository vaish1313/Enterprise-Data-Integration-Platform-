package com.company.integrationplatform.ingestion;

import com.company.integrationplatform.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ingestion_jobs")
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

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "records_processed")
    @Builder.Default
    private int recordsProcessed = 0;

    @Column(name = "records_failed")
    @Builder.Default
    private int recordsFailed = 0;

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
