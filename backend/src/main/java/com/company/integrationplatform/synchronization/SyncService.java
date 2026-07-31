package com.company.integrationplatform.synchronization;

import com.company.integrationplatform.common.PageResponse;
import com.company.integrationplatform.circuitbreaker.CircuitBreakerService;
import com.company.integrationplatform.datasource.DataSourceEntity;
import com.company.integrationplatform.datasource.DataSourceRepository;
import com.company.integrationplatform.exception.ResourceNotFoundException;
import com.company.integrationplatform.ingestion.IngestionRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service layer for the Synchronization Engine.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Scheduled sync every 5 minutes via {@link #runScheduledSync()}</li>
 *   <li>Manual sync trigger via {@link #runSync(UUID)}</li>
 *   <li>Job queries (list, get by ID, get by source)</li>
 *   <li>Platform-wide statistics</li>
 * </ul>
 *
 * <p>All actual sync logic is delegated to {@link SyncExecutor} — a separate
 * Spring bean — so {@code @Transactional} is applied correctly via the proxy.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SyncService {

    private final SyncRepository            syncRepository;
    private final DataSourceRepository      dataSourceRepository;
    private final IngestionRecordRepository recordRepository;
    private final SyncExecutor              syncExecutor;
    private final CircuitBreakerService     circuitBreakerService;

    // ─────────────────────────────────────────────────────────────────────────
    // SCHEDULER — every 5 minutes
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Scheduled synchronization — runs every 5 minutes.
     *
     * <p>Fetches all schedulable data sources (ACTIVE, DEGRADED, SUSPENDED) and
     * processes each one independently. Before attempting a sync:
     * <ol>
     *   <li>Calls {@link CircuitBreakerService#transitionToHalfOpenIfReady} to promote
     *       OPEN sources whose suspension window has elapsed to HALF_OPEN.</li>
     *   <li>Calls {@link CircuitBreakerService#isOpen} — if still OPEN, skips entirely
     *       (no job record created, no executor call).</li>
     * </ol>
     *
     * <p>Uses {@code fixedDelayString} so the next run starts 5 minutes
     * after the previous one completes (not a fixed-rate overlap).
     */
    @Scheduled(fixedDelayString = "${sync.interval-ms:300000}")
    public void runScheduledSync() {
        log.info("Scheduled sync started at {}", LocalDateTime.now());

        // Fetch ACTIVE + DEGRADED + SUSPENDED sources (SUSPENDED needed for auto-recovery check)
        List<DataSourceEntity> candidateSources = dataSourceRepository.findSchedulableSources();

        if (candidateSources.isEmpty()) {
            log.info("Scheduled sync: no schedulable data sources found, skipping.");
            return;
        }

        log.info("Scheduled sync: evaluating {} candidate source(s)", candidateSources.size());
        int skipped = 0, attempted = 0;

        for (DataSourceEntity source : candidateSources) {

            // ── Step 1: Promote OPEN → HALF_OPEN if suspension window has elapsed ──
            circuitBreakerService.transitionToHalfOpenIfReady(source);

            // ── Step 2: Skip if circuit is still OPEN ─────────────────────────
            if (circuitBreakerService.isOpen(source)) {
                log.info("Scheduled sync: circuit OPEN for '{}', skipping (suspended until {}).",
                        source.getName(), source.getSuspendedUntil());
                skipped++;
                continue; // ← No job record created. Retry logic untouched.
            }

            // ── Step 3: Attempt sync (CLOSED or HALF_OPEN test attempt) ───────
            attempted++;
            try {
                SyncDto.SyncReport report = syncExecutor.execute(source.getId(), "SCHEDULER").join();
                if (report != null) {
                    log.info("Scheduled sync completed for source '{}': {}",
                            source.getName(), report.getSummary());
                }
            } catch (Exception e) {
                log.error("Scheduled sync failed for source '{}': {}",
                        source.getName(), e.getMessage());
            }
        }

        log.info("Scheduled sync finished at {}. Attempted={}, Skipped (circuit OPEN)={}.",
                LocalDateTime.now(), attempted, skipped);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MANUAL TRIGGER
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Manually triggers a synchronization run for a specific data source.
     *
     * @param dataSourceId the data source to synchronize
     * @return a {@link SyncDto.SyncReport} with full job details
     */
    public java.util.concurrent.CompletableFuture<SyncDto.SyncReport> runSync(UUID dataSourceId) {
        String currentUser = currentUsername();
        log.info("Manual sync triggered: dataSourceId={}, user={}", dataSourceId, currentUser);
        return syncExecutor.execute(dataSourceId, currentUser);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // READ — JOBS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns a paginated list of all sync jobs, newest first.
     */
    @Transactional(readOnly = true)
    public PageResponse<SyncDto.JobResponse> getAllJobs(Pageable pageable) {
        Page<SyncDto.JobResponse> page = syncRepository
                .findAll(pageable)
                .map(SyncDto.JobResponse::from);
        return PageResponse.of(page);
    }

    /**
     * Returns full details of a single sync job.
     *
     * @throws ResourceNotFoundException if no job exists with the given ID
     */
    @Transactional(readOnly = true)
    public SyncDto.JobResponse getJobById(UUID id) {
        return SyncDto.JobResponse.from(
                syncRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("SyncJob", id)));
    }

    /**
     * Returns paginated sync jobs for a specific data source.
     */
    @Transactional(readOnly = true)
    public PageResponse<SyncDto.JobResponse> getJobsBySource(UUID dataSourceId, Pageable pageable) {
        Page<SyncDto.JobResponse> page = syncRepository
                .findByDataSourceId(dataSourceId, pageable)
                .map(SyncDto.JobResponse::from);
        return PageResponse.of(page);
    }

    /**
     * Returns the N most recent sync jobs across all data sources.
     */
    @Transactional(readOnly = true)
    public List<SyncDto.JobResponse> getRecentJobs(int limit) {
        return syncRepository
                .findLatestJobs(PageRequest.of(0, limit, Sort.by("startedAt").descending()))
                .stream()
                .map(SyncDto.JobResponse::from)
                .toList();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // STATISTICS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns platform-wide synchronization statistics.
     */
    @Transactional(readOnly = true)
    public SyncDto.StatisticsResponse getStatistics() {
        long totalJobs     = syncRepository.count();
        long completedJobs = syncRepository.countByStatus(SyncJob.SyncStatus.COMPLETED);
        long failedJobs    = syncRepository.countByStatus(SyncJob.SyncStatus.FAILED);
        long runningJobs   = syncRepository.countByStatus(SyncJob.SyncStatus.RUNNING);
        long pendingJobs   = syncRepository.countByStatus(SyncJob.SyncStatus.PENDING);

        long totalSynced   = syncRepository.sumTotalRecordsProcessed();
        long totalFailed   = syncRepository.sumTotalRecordsFailed();
        long pendingSync   = recordRepository.countPendingSync();
        long alreadySynced = recordRepository.countSynchronized();

        Double avgExecDouble = syncRepository.avgExecutionTimeMs();
        long avgExecMs = avgExecDouble != null ? avgExecDouble.longValue() : 0L;
        long maxExecMs = syncRepository.maxExecutionTimeMs();

        LocalDateTime lastSuccessAt = syncRepository.findLastSuccessfulSync()
                .map(SyncJob::getCompletedAt)
                .orElse(null);

        double successRate = totalJobs == 0 ? 0.0
                : Math.round((completedJobs * 100.0 / totalJobs) * 10.0) / 10.0;

        return SyncDto.StatisticsResponse.builder()
                .totalJobs(totalJobs)
                .completedJobs(completedJobs)
                .failedJobs(failedJobs)
                .runningJobs(runningJobs)
                .pendingJobs(pendingJobs)
                .totalRecordsSynchronized(totalSynced)
                .totalRecordsFailed(totalFailed)
                .recordsPendingSync(pendingSync)
                .recordsAlreadySynchronized(alreadySynced)
                .avgExecutionTimeMs(avgExecMs)
                .maxExecutionTimeMs(maxExecMs)
                .lastSuccessfulSyncAt(lastSuccessAt)
                .successRatePercent(successRate)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPER
    // ─────────────────────────────────────────────────────────────────────────

    private String currentUsername() {
        try {
            return SecurityContextHolder.getContext().getAuthentication().getName();
        } catch (Exception e) {
            return "SYSTEM";
        }
    }
}
