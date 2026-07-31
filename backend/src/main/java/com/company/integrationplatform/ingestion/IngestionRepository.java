package com.company.integrationplatform.ingestion;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface IngestionRepository extends JpaRepository<IngestionJob, UUID> {

    // ── Filtered lookups ──────────────────────────────────────────────────────

    Page<IngestionJob> findByDataSourceId(UUID dataSourceId, Pageable pageable);

    Page<IngestionJob> findByStatus(IngestionJob.JobStatus status, Pageable pageable);

    Page<IngestionJob> findByIngestionType(IngestionJob.IngestionType type, Pageable pageable);

    List<IngestionJob> findByDataSourceId(UUID dataSourceId);

    List<IngestionJob> findByStatus(IngestionJob.JobStatus status);

    // ── Dashboard aggregates ──────────────────────────────────────────────────

    long countByStatus(IngestionJob.JobStatus status);

    @Query("SELECT COALESCE(SUM(j.recordsProcessed), 0) FROM IngestionJob j WHERE j.status = 'COMPLETED'")
    Long sumSuccessfulRecords();

    @Query("SELECT COALESCE(SUM(j.recordsFailed), 0) FROM IngestionJob j")
    Long sumFailedRecords();

    @Query("SELECT COALESCE(SUM(j.totalRecords), 0) FROM IngestionJob j")
    Long sumTotalRecords();

    @Query("SELECT COALESCE(AVG(j.recordsProcessed), 0) FROM IngestionJob j WHERE j.status = 'COMPLETED'")
    double avgRecordsPerJob();

    @Query("SELECT j FROM IngestionJob j ORDER BY j.createdAt DESC")
    List<IngestionJob> findLatestJobs(org.springframework.data.domain.Pageable pageable);

    // ── Type breakdown (dashboard) ────────────────────────────────────────────

    long countByIngestionType(IngestionJob.IngestionType type);

    // ── Metrics ───────────────────────────────────────────────────────────────

    @Query("SELECT COALESCE(AVG(j.retryCount), 0.0) FROM IngestionJob j")
    Double getAverageRetryCount();

    @Query(value = "SELECT COALESCE(AVG(EXTRACT(EPOCH FROM (completed_at - started_at)) * 1000), 0) FROM ingestion_jobs WHERE completed_at IS NOT NULL AND started_at IS NOT NULL", nativeQuery = true)
    Double getAverageProcessingTimeMs();
}
