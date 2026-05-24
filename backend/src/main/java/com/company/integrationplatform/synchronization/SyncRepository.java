package com.company.integrationplatform.synchronization;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SyncRepository extends JpaRepository<SyncJob, UUID> {

    // ── Filtered lookups ──────────────────────────────────────────────────────

    Page<SyncJob> findByDataSourceId(UUID dataSourceId, Pageable pageable);

    Page<SyncJob> findByStatus(SyncJob.SyncStatus status, Pageable pageable);

    List<SyncJob> findByDataSourceId(UUID dataSourceId);

    // ── Latest / recent ───────────────────────────────────────────────────────

    @Query("SELECT j FROM SyncJob j ORDER BY j.startedAt DESC")
    List<SyncJob> findLatestJobs(Pageable pageable);

    @Query("SELECT j FROM SyncJob j WHERE j.status = 'COMPLETED' ORDER BY j.completedAt DESC")
    Optional<SyncJob> findLastSuccessfulSync();

    @Query("SELECT j FROM SyncJob j WHERE j.dataSourceId = :dataSourceId ORDER BY j.startedAt DESC")
    Optional<SyncJob> findLatestByDataSourceId(UUID dataSourceId);

    // ── Statistics aggregates ─────────────────────────────────────────────────

    long countByStatus(SyncJob.SyncStatus status);

    @Query("SELECT COALESCE(SUM(j.recordsProcessed), 0) FROM SyncJob j WHERE j.status = 'COMPLETED'")
    long sumTotalRecordsProcessed();

    @Query("SELECT COALESCE(SUM(j.recordsFailed), 0) FROM SyncJob j")
    long sumTotalRecordsFailed();

    @Query("SELECT COALESCE(AVG(j.executionTimeMs), 0) FROM SyncJob j WHERE j.executionTimeMs IS NOT NULL")
    double avgExecutionTimeMs();

    @Query("SELECT COALESCE(MAX(j.executionTimeMs), 0) FROM SyncJob j WHERE j.executionTimeMs IS NOT NULL")
    long maxExecutionTimeMs();
}
