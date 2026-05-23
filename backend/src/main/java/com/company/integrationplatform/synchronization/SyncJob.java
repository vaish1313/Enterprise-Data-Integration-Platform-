package com.company.integrationplatform.synchronization;

import com.company.integrationplatform.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "sync_jobs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SyncJob extends BaseEntity {

    @Column(name = "data_source_id", nullable = false)
    private UUID dataSourceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private SyncStatus status = SyncStatus.PENDING;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "records_processed")
    @Builder.Default
    private int recordsProcessed = 0;

    @Column(name = "records_failed")
    @Builder.Default
    private int recordsFailed = 0;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "triggered_by", length = 100)
    @Builder.Default
    private String triggeredBy = "SCHEDULER";

    public enum SyncStatus {
        PENDING, RUNNING, COMPLETED, FAILED, PARTIAL
    }
}
