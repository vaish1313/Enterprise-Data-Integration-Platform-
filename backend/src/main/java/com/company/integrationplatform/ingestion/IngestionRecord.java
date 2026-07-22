package com.company.integrationplatform.ingestion;

import com.company.integrationplatform.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;
import java.util.UUID;

/**
 * Represents a single parsed row from an ingestion job.
 * Stores both the raw CSV data and the transformed output.
 * Failed rows store an error message and optional column name for diagnostics.
 */
@Entity
@Table(name = "ingestion_records",
        indexes = {
                @Index(name = "idx_ingest_rec_job",    columnList = "job_id"),
                @Index(name = "idx_ingest_rec_status", columnList = "status")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IngestionRecord extends BaseEntity {

    @Column(name = "job_id", nullable = false)
    private UUID jobId;

    @Column(name = "data_source_id", nullable = false)
    private UUID dataSourceId;

    /** Raw key-value pairs parsed directly from the CSV row. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_data", columnDefinition = "jsonb")
    private Map<String, Object> rawData;

    /** Transformed output after applying transformation rules. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "transformed_data", columnDefinition = "jsonb")
    private Map<String, Object> transformedData;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private RecordStatus status = RecordStatus.PENDING;

    /** Human-readable error description for FAILED records. */
    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    /** The specific column name that caused a validation failure (if applicable). */
    @Column(name = "column_name", length = 255)
    private String columnName;

    /** 1-based row number in the source CSV (header = row 0, first data row = 1). */
    @Column(name = "source_row_number")
    private Integer sourceRowNumber;

    /**
     * Whether this record has been picked up and processed by the sync engine.
     * Set to true after a successful sync job processes this record.
     */
    @Column(name = "is_synchronized", nullable = false)
    @Builder.Default
    private boolean synchronized_ = false;

    /**
     * ID of the sync job that last processed this record.
     */
    @Column(name = "sync_job_id")
    private UUID syncJobId;

    public enum RecordStatus {
        PENDING, PROCESSED, FAILED, SKIPPED
    }
}
