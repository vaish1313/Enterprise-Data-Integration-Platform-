package com.company.integrationplatform.datasource;

import com.company.integrationplatform.circuitbreaker.CircuitState;
import com.company.integrationplatform.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "data_sources")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DataSourceEntity extends BaseEntity {

    @Column(nullable = false, length = 255)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 50)
    private SourceType sourceType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "connection_details", columnDefinition = "jsonb")
    private Map<String, String> connectionDetails;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private SourceStatus status = SourceStatus.INACTIVE;

    @Column(length = 500)
    private String description;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    // ── Circuit Breaker fields (added in V6 migration) ────────────────────────

    /**
     * The current circuit breaker state for this data source.
     * CLOSED = normal, OPEN = suspended (scheduler skips), HALF_OPEN = one test allowed.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "circuit_state", nullable = false, length = 20)
    @Builder.Default
    private CircuitState circuitState = CircuitState.CLOSED;

    /**
     * Number of consecutive permanently-failed jobs since the last successful job.
     * Reset to 0 on any success. Circuit opens when this reaches 3.
     */
    @Column(name = "consecutive_failure_count", nullable = false)
    @Builder.Default
    private int consecutiveFailureCount = 0;

    /** Timestamp of the most recent permanently-failed job. Null if circuit is clean. */
    @Column(name = "last_failure_at")
    private LocalDateTime lastFailureAt;

    /**
     * Auto-recovery deadline. When {@code NOW() > suspended_until} and
     * {@code circuit_state = OPEN}, the scheduler transitions to HALF_OPEN.
     * Null when the circuit is CLOSED.
     */
    @Column(name = "suspended_until")
    private LocalDateTime suspendedUntil;

    // ─────────────────────────────────────────────────────────────────────────

    public enum SourceType {
        CSV, REST_API, DATABASE
    }

    /**
     * Operational status of the data source.
     * DEGRADED and SUSPENDED are managed exclusively by {@code CircuitBreakerService}
     * and are never set directly by user requests.
     */
    public enum SourceStatus {
        /** Normal, schedulable operation. */
        ACTIVE,
        /** Manually deactivated by an admin. */
        INACTIVE,
        /** A transient or configuration error was detected. */
        ERROR,
        /** Circuit is HALF_OPEN — cautiously attempting one test run. */
        DEGRADED,
        /** Circuit is OPEN — scheduler skips this source entirely. */
        SUSPENDED
    }
}
