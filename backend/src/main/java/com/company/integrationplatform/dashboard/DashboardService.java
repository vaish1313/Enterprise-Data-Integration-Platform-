package com.company.integrationplatform.dashboard;

import com.company.integrationplatform.audit.AuditEntity;
import com.company.integrationplatform.audit.AuditRepository;
import com.company.integrationplatform.datasource.DataSourceEntity;
import com.company.integrationplatform.datasource.DataSourceRepository;
import com.company.integrationplatform.ingestion.IngestionJob;
import com.company.integrationplatform.ingestion.IngestionRecordRepository;
import com.company.integrationplatform.ingestion.IngestionRepository;
import com.company.integrationplatform.synchronization.SyncJob;
import com.company.integrationplatform.synchronization.SyncRepository;
import com.company.integrationplatform.transformation.TransformationRepository;
import com.company.integrationplatform.user.entity.User;
import com.company.integrationplatform.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Service layer for the Dashboard Analytics module.
 *
 * <p>All methods are read-only transactions. Each method issues a focused set of
 * aggregate queries — no full entity loads — to keep response times low even on
 * large datasets.
 *
 * <p><b>Endpoints served:</b>
 * <ul>
 *   <li>{@link #getOverview()}         → GET /api/v1/dashboard/overview</li>
 *   <li>{@link #getIngestionStats()}   → GET /api/v1/dashboard/ingestion</li>
 *   <li>{@link #getSyncStats()}        → GET /api/v1/dashboard/synchronization</li>
 *   <li>{@link #getAuditStats()}       → GET /api/v1/dashboard/audit</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final UserRepository            userRepository;
    private final DataSourceRepository      dataSourceRepository;
    private final IngestionRepository       ingestionRepository;
    private final IngestionRecordRepository recordRepository;
    private final TransformationRepository  transformationRepository;
    private final SyncRepository            syncRepository;
    private final AuditRepository           auditRepository;

    // ─────────────────────────────────────────────────────────────────────────
    // OVERVIEW  —  platform-wide KPI snapshot
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns a platform-wide KPI snapshot covering users, data sources,
     * ingestion, transformation, and synchronization totals.
     *
     * <p>Uses only aggregate COUNT/SUM queries — no entity collections are loaded.
     */
    @Transactional(readOnly = true)
    public DashboardDto.OverviewStats getOverview() {
        log.debug("Building dashboard overview stats");

        // ── Users ─────────────────────────────────────────────────────────────
        long totalUsers    = userRepository.count();
        long activeUsers   = userRepository.countByEnabledTrue();
        long adminUsers    = userRepository.countByRole(User.Role.ADMIN);
        long analystUsers  = userRepository.countByRole(User.Role.ANALYST);
        long operatorUsers = userRepository.countByRole(User.Role.OPERATOR);

        // ── Data Sources ──────────────────────────────────────────────────────
        long totalDs    = dataSourceRepository.count();
        long activeDs   = dataSourceRepository.countByStatus(DataSourceEntity.SourceStatus.ACTIVE);
        long inactiveDs = dataSourceRepository.countByStatus(DataSourceEntity.SourceStatus.INACTIVE);
        long errorDs    = dataSourceRepository.countByStatus(DataSourceEntity.SourceStatus.ERROR);

        // ── Ingestion ─────────────────────────────────────────────────────────
        long totalIngestion      = ingestionRepository.count();
        long successfulIngestion = ingestionRepository.countByStatus(IngestionJob.JobStatus.COMPLETED);
        long failedIngestion     = ingestionRepository.countByStatus(IngestionJob.JobStatus.FAILED);
        Long totalImportedRaw    = ingestionRepository.sumTotalRecords();
        long totalImported       = totalImportedRaw != null ? totalImportedRaw : 0L;

        // ── Transformation ────────────────────────────────────────────────────
        long totalRules  = transformationRepository.count();
        long activeRules = transformationRepository.countByActiveTrue();

        // ── Synchronization ───────────────────────────────────────────────────
        long totalSync     = syncRepository.count();
        long successSync   = syncRepository.countByStatus(SyncJob.SyncStatus.COMPLETED);
        long failedSync    = syncRepository.countByStatus(SyncJob.SyncStatus.FAILED);

        LocalDateTime lastSyncTime = syncRepository
                .findLastSuccessfulSync()
                .map(SyncJob::getCompletedAt)
                .orElse(null);

        double syncSuccessPct = totalSync == 0 ? 0.0
                : round((successSync * 100.0) / totalSync);
        double ingestionSuccessPct = totalIngestion == 0 ? 0.0
                : round((successfulIngestion * 100.0) / totalIngestion);

        return DashboardDto.OverviewStats.builder()
                .totalUsers(totalUsers)
                .activeUsers(activeUsers)
                .adminUsers(adminUsers)
                .analystUsers(analystUsers)
                .operatorUsers(operatorUsers)
                .totalDataSources(totalDs)
                .activeDataSources(activeDs)
                .inactiveDataSources(inactiveDs)
                .errorDataSources(errorDs)
                .totalIngestionJobs(totalIngestion)
                .successfulIngestionJobs(successfulIngestion)
                .failedIngestionJobs(failedIngestion)
                .totalImportedRecords(totalImported)
                .totalTransformationRules(totalRules)
                .activeTransformationRules(activeRules)
                .totalSyncJobs(totalSync)
                .successfulSyncJobs(successSync)
                .failedSyncJobs(failedSync)
                .lastSynchronizationTime(lastSyncTime)
                .syncSuccessPercent(syncSuccessPct)
                .ingestionSuccessPercent(ingestionSuccessPct)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // INGESTION  —  detailed ingestion analytics
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns detailed ingestion analytics: job counts by status, record totals,
     * sync readiness, success rate, and per-type breakdowns.
     */
    @Transactional(readOnly = true)
    public DashboardDto.IngestionStats getIngestionStats() {
        log.debug("Building ingestion stats");

        // ── Job counts ────────────────────────────────────────────────────────
        long total     = ingestionRepository.count();
        long completed = ingestionRepository.countByStatus(IngestionJob.JobStatus.COMPLETED);
        long failed    = ingestionRepository.countByStatus(IngestionJob.JobStatus.FAILED);
        long running   = ingestionRepository.countByStatus(IngestionJob.JobStatus.RUNNING);
        long pending   = ingestionRepository.countByStatus(IngestionJob.JobStatus.PENDING);
        long partial   = ingestionRepository.countByStatus(IngestionJob.JobStatus.PARTIAL);

        // ── Record counts ─────────────────────────────────────────────────────
        long totalRecords     = ingestionRepository.sumTotalRecords();
        Long processedRaw     = ingestionRepository.sumSuccessfulRecords();
        Long failedRaw        = ingestionRepository.sumFailedRecords();
        long processedRecords = processedRaw != null ? processedRaw : 0L;
        long failedRecords    = failedRaw    != null ? failedRaw    : 0L;
        long pendingSync      = recordRepository.countPendingSync();
        long alreadySynced    = recordRepository.countSynchronized();

        // ── Rates ─────────────────────────────────────────────────────────────
        double jobSuccessPct  = total == 0 ? 0.0 : round((completed * 100.0) / total);
        double avgPerJob      = Math.round(ingestionRepository.avgRecordsPerJob() * 10.0) / 10.0;

        // ── Type breakdown ────────────────────────────────────────────────────
        long csvJobs       = ingestionRepository.countByIngestionType(IngestionJob.IngestionType.CSV);
        long apiJobs       = ingestionRepository.countByIngestionType(IngestionJob.IngestionType.REST_API);
        long scheduledJobs = ingestionRepository.countByIngestionType(IngestionJob.IngestionType.SCHEDULED);

        return DashboardDto.IngestionStats.builder()
                .totalJobs(total)
                .completedJobs(completed)
                .failedJobs(failed)
                .runningJobs(running)
                .pendingJobs(pending)
                .partialJobs(partial)
                .totalRecords(totalRecords)
                .processedRecords(processedRecords)
                .failedRecords(failedRecords)
                .recordsPendingSync(pendingSync)
                .recordsSynchronized(alreadySynced)
                .jobSuccessPercent(jobSuccessPct)
                .avgRecordsPerJob(avgPerJob)
                .csvJobs(csvJobs)
                .apiJobs(apiJobs)
                .scheduledJobs(scheduledJobs)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SYNCHRONIZATION  —  detailed sync analytics
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns detailed synchronization analytics: job counts by status, record totals,
     * execution time statistics, last sync timestamp, and success rate.
     */
    @Transactional(readOnly = true)
    public DashboardDto.SynchronizationStats getSyncStats() {
        log.debug("Building synchronization stats");

        // ── Job counts ────────────────────────────────────────────────────────
        long total     = syncRepository.count();
        long completed = syncRepository.countByStatus(SyncJob.SyncStatus.COMPLETED);
        long failed    = syncRepository.countByStatus(SyncJob.SyncStatus.FAILED);
        long running   = syncRepository.countByStatus(SyncJob.SyncStatus.RUNNING);
        long pending   = syncRepository.countByStatus(SyncJob.SyncStatus.PENDING);

        // ── Record counts ─────────────────────────────────────────────────────
        long totalSynced  = syncRepository.sumTotalRecordsProcessed();
        long totalFailed  = syncRepository.sumTotalRecordsFailed();
        long pendingSync  = recordRepository.countPendingSync();

        // ── Timing ────────────────────────────────────────────────────────────
        LocalDateTime lastSyncTime = syncRepository
                .findLastSuccessfulSync()
                .map(SyncJob::getCompletedAt)
                .orElse(null);
        Double avgExecRaw = syncRepository.avgExecutionTimeMs();
        Long   maxExecRaw = syncRepository.maxExecutionTimeMs();
        long avgExecMs = avgExecRaw != null ? avgExecRaw.longValue() : 0L;
        long maxExecMs = maxExecRaw != null ? maxExecRaw             : 0L;

        // ── Rate ──────────────────────────────────────────────────────────────
        double successPct = total == 0 ? 0.0 : round((completed * 100.0) / total);

        return DashboardDto.SynchronizationStats.builder()
                .totalJobs(total)
                .completedJobs(completed)
                .failedJobs(failed)
                .runningJobs(running)
                .pendingJobs(pending)
                .totalRecordsSynchronized(totalSynced)
                .totalRecordsFailed(totalFailed)
                .recordsPendingSync(pendingSync)
                .lastSynchronizationTime(lastSyncTime)
                .avgExecutionTimeMs(avgExecMs)
                .maxExecutionTimeMs(maxExecMs)
                .successPercent(successPct)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // AUDIT  —  audit log analytics
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns audit log analytics: total events, success/failure counts,
     * distinct active users, event success rate, a breakdown by action type,
     * and the 10 most recent audit entries.
     */
    @Transactional(readOnly = true)
    public DashboardDto.AuditStats getAuditStats() {
        log.debug("Building audit stats");

        // ── Counts ────────────────────────────────────────────────────────────
        long total      = auditRepository.count();
        long successful = auditRepository.countSuccessfulActions();
        long failed     = auditRepository.countFailedActions();
        long distinct   = auditRepository.countDistinctUsers();

        // ── Rate ──────────────────────────────────────────────────────────────
        double successPct = total == 0 ? 0.0 : round((successful * 100.0) / total);

        // ── Action breakdown ──────────────────────────────────────────────────
        List<Object[]> grouped = auditRepository.countGroupedByAction();
        Map<String, Long> byAction = new LinkedHashMap<>();
        for (Object[] row : grouped) {
            byAction.put((String) row[0], (Long) row[1]);
        }

        // ── Recent events (top 10) ────────────────────────────────────────────
        List<AuditEntity> recent = auditRepository.findLatestEntries(PageRequest.of(0, 10));
        List<DashboardDto.RecentAuditEntry> recentEntries = recent.stream()
                .map(e -> DashboardDto.RecentAuditEntry.builder()
                        .action(e.getAction())
                        .username(e.getUsername())
                        .status(e.getStatus())
                        .details(e.getDetails())
                        .timestamp(e.getTimestamp())
                        .build())
                .toList();

        return DashboardDto.AuditStats.builder()
                .totalEvents(total)
                .successfulEvents(successful)
                .failedEvents(failed)
                .distinctActiveUsers(distinct)
                .eventSuccessPercent(successPct)
                .eventsByAction(byAction)
                .recentEvents(recentEntries)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPER
    // ─────────────────────────────────────────────────────────────────────────

    /** Rounds a percentage to one decimal place. */
    private double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
