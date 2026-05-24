package com.company.integrationplatform.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface AuditRepository extends JpaRepository<AuditEntity, UUID> {

    // ── Read queries ──────────────────────────────────────────────────────────

    Page<AuditEntity> findByUsername(String username, Pageable pageable);

    Page<AuditEntity> findByAction(String action, Pageable pageable);

    Page<AuditEntity> findByTimestampBetween(LocalDateTime from, LocalDateTime to, Pageable pageable);

    Page<AuditEntity> findByUsernameAndAction(String username, String action, Pageable pageable);

    // ── Export queries (unbounded — used for CSV streaming) ───────────────────

    List<AuditEntity> findAllByOrderByTimestampDesc();

    List<AuditEntity> findByTimestampBetweenOrderByTimestampDesc(LocalDateTime from, LocalDateTime to);

    // ── Delete / purge queries ────────────────────────────────────────────────

    /**
     * Bulk-deletes all audit entries with a timestamp older than the given cutoff.
     * Returns the number of rows deleted.
     */
    @Modifying
    @Query("DELETE FROM AuditEntity a WHERE a.timestamp < :cutoff")
    int deleteByTimestampBefore(LocalDateTime cutoff);

    // ── Dashboard aggregate queries ───────────────────────────────────────────

    @Query("SELECT COUNT(a) FROM AuditEntity a WHERE a.status = 'SUCCESS'")
    long countSuccessfulActions();

    @Query("SELECT COUNT(a) FROM AuditEntity a WHERE a.status = 'FAILED'")
    long countFailedActions();

    @Query("SELECT COUNT(a) FROM AuditEntity a WHERE a.action = :action")
    long countByAction(String action);

    @Query("SELECT COUNT(DISTINCT a.username) FROM AuditEntity a")
    long countDistinctUsers();

    @Query("SELECT a FROM AuditEntity a ORDER BY a.timestamp DESC")
    List<AuditEntity> findLatestEntries(Pageable pageable);

    @Query("SELECT a.action, COUNT(a) FROM AuditEntity a GROUP BY a.action ORDER BY COUNT(a) DESC")
    List<Object[]> countGroupedByAction();
}
