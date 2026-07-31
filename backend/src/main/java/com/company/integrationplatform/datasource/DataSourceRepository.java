package com.company.integrationplatform.datasource;

import com.company.integrationplatform.circuitbreaker.CircuitState;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@link DataSourceEntity}.
 * Extends {@link JpaSpecificationExecutor} to support dynamic search queries
 * built via {@link DataSourceSpecification}.
 */
@Repository
public interface DataSourceRepository
        extends JpaRepository<DataSourceEntity, UUID>,
                JpaSpecificationExecutor<DataSourceEntity> {

    // ── Lookup ────────────────────────────────────────────────────────────────

    Optional<DataSourceEntity> findByName(String name);

    boolean existsByName(String name);

    /** Case-insensitive exact name check — used for duplicate detection on update. */
    boolean existsByNameIgnoreCaseAndIdNot(String name, UUID id);

    // ── Filter queries ────────────────────────────────────────────────────────

    Page<DataSourceEntity> findByStatus(DataSourceEntity.SourceStatus status, Pageable pageable);

    Page<DataSourceEntity> findBySourceType(DataSourceEntity.SourceType sourceType, Pageable pageable);

    List<DataSourceEntity> findByStatus(DataSourceEntity.SourceStatus status);

    // ── Dashboard / analytics counts ─────────────────────────────────────────

    long countByStatus(DataSourceEntity.SourceStatus status);

    long countBySourceType(DataSourceEntity.SourceType sourceType);

    /** Returns the total number of active REST_API sources (used by the scheduler). */
    @Query("SELECT COUNT(d) FROM DataSourceEntity d WHERE d.status = 'ACTIVE' AND d.sourceType = 'REST_API'")
    long countActiveApiSources();

    // ── Circuit breaker queries ───────────────────────────────────────────────

    /**
     * Returns all data sources the scheduler should consider each tick.
     * Includes ACTIVE (CLOSED circuit), DEGRADED (HALF_OPEN test attempt), and
     * SUSPENDED sources so the circuit breaker can auto-transition OPEN → HALF_OPEN.
     * Sources with circuit_state = OPEN and a future suspended_until are filtered
     * out by {@code CircuitBreakerService.isOpen()} after this call.
     */
    @Query("SELECT d FROM DataSourceEntity d WHERE d.status IN ('ACTIVE', 'DEGRADED', 'SUSPENDED')")
    List<DataSourceEntity> findSchedulableSources();

    /** Used by CircuitBreakerService to look up all OPEN circuits (e.g. for admin dashboards). */
    List<DataSourceEntity> findByCircuitState(CircuitState circuitState);
}
