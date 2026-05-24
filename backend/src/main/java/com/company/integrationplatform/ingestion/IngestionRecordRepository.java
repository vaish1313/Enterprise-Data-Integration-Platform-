package com.company.integrationplatform.ingestion;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface IngestionRecordRepository extends JpaRepository<IngestionRecord, UUID> {

    Page<IngestionRecord> findByJobId(UUID jobId, Pageable pageable);

    List<IngestionRecord> findByJobId(UUID jobId);

    Page<IngestionRecord> findByJobIdAndStatus(UUID jobId, IngestionRecord.RecordStatus status,
                                               Pageable pageable);

    long countByJobIdAndStatus(UUID jobId, IngestionRecord.RecordStatus status);

    void deleteByJobId(UUID jobId);

    // ── Sync engine queries ───────────────────────────────────────────────────

    /**
     * Finds all PROCESSED records that have not yet been synchronized,
     * scoped to a specific data source. Used by the sync engine to pick up
     * pending work.
     */
    @Query("""
            SELECT r FROM IngestionRecord r
            WHERE r.dataSourceId = :dataSourceId
              AND r.status = 'PROCESSED'
              AND r.synchronized_ = false
            ORDER BY r.createdAt ASC
            """)
    List<IngestionRecord> findPendingSyncRecords(UUID dataSourceId);

    /**
     * Counts unsynced PROCESSED records for a data source.
     */
    @Query("""
            SELECT COUNT(r) FROM IngestionRecord r
            WHERE r.dataSourceId = :dataSourceId
              AND r.status = 'PROCESSED'
              AND r.synchronized_ = false
            """)
    long countPendingSyncRecords(UUID dataSourceId);

    /**
     * Bulk-marks records as synchronized after a successful sync job.
     */
    @Modifying
    @Query("""
            UPDATE IngestionRecord r
            SET r.synchronized_ = true, r.syncJobId = :syncJobId
            WHERE r.id IN :ids
            """)
    void markAsSynchronized(List<UUID> ids, UUID syncJobId);

    /**
     * Total count of synchronized records across all data sources.
     */
    @Query("SELECT COUNT(r) FROM IngestionRecord r WHERE r.synchronized_ = true")
    long countSynchronized();

    /**
     * Total count of records pending synchronization across all data sources.
     */
    @Query("SELECT COUNT(r) FROM IngestionRecord r WHERE r.status = 'PROCESSED' AND r.synchronized_ = false")
    long countPendingSync();
}
