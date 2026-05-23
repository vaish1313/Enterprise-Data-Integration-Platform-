package com.company.integrationplatform.ingestion;

import com.company.integrationplatform.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "ingestion_records")
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

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_data", columnDefinition = "jsonb")
    private Map<String, Object> rawData;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "transformed_data", columnDefinition = "jsonb")
    private Map<String, Object> transformedData;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private RecordStatus status = RecordStatus.PENDING;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "source_row_number")
    private Integer sourceRowNumber;

    public enum RecordStatus {
        PENDING, PROCESSED, FAILED, SKIPPED
    }
}
