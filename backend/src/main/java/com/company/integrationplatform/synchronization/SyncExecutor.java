package com.company.integrationplatform.synchronization;

import com.company.integrationplatform.audit.AuditService;
import com.company.integrationplatform.common.Constants;
import com.company.integrationplatform.datasource.DataSourceEntity;
import com.company.integrationplatform.datasource.DataSourceNotFoundException;
import com.company.integrationplatform.datasource.DataSourceRepository;
import com.company.integrationplatform.ingestion.IngestionRecord;
import com.company.integrationplatform.ingestion.IngestionRecordRepository;
import com.company.integrationplatform.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Core synchronization engine.
 *
 * <p>Extracted into a separate Spring bean so {@code @Transactional} is applied
 * via the proxy — avoids the self-invocation bypass problem in {@link SyncService}.
 *
 * <p><b>Workflow per execution:</b>
 * <ol>
 *   <li>Create a {@link SyncJob} with status {@code RUNNING}</li>
 *   <li>Fetch all PROCESSED, un-synchronized records for the data source</li>
 *   <li>Validate each record via {@link SyncRecordValidator}</li>
 *   <li>Mark validated records as synchronized (batch update)</li>
 *   <li>Finalize job with counts, execution time, and status</li>
 *   <li>Generate a {@link SyncDto.SyncReport}</li>
 *   <li>Emit audit events</li>
 * </ol>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SyncExecutor {

    private final SyncRepository             syncRepository;
    private final DataSourceRepository       dataSourceRepository;
    private final IngestionRecordRepository  recordRepository;
    private final SyncRecordValidator        validator;
    private final AuditService               auditService;
    private final NotificationService        notificationService;

    /**
     * Executes a full synchronization run for the given data source.
     *
     * @param dataSourceId the data source to synchronize
     * @param triggeredBy  "SCHEDULER" or the username who triggered manually
     * @return a {@link SyncDto.SyncReport} with full job details and summary
     */
    @org.springframework.scheduling.annotation.Async("jobExecutor")
    public java.util.concurrent.CompletableFuture<SyncDto.SyncReport> execute(UUID dataSourceId, String triggeredBy) {
        long wallStart = System.currentTimeMillis();

        // ── Step 1: Validate data source exists ───────────────────────────────
        DataSourceEntity source = dataSourceRepository.findById(dataSourceId)
                .orElseThrow(() -> new DataSourceNotFoundException(dataSourceId));

        // ── Step 2: Create job record ─────────────────────────────────────────
        SyncJob job = SyncJob.builder()
                .dataSourceId(dataSourceId)
                .status(SyncJob.SyncStatus.RUNNING)
                .startedAt(LocalDateTime.now())
                .triggeredBy(triggeredBy)
                .build();
        job = syncRepository.save(job);

        auditService.log(
                Constants.ACTION_SYNC_STARTED,
                triggeredBy,
                "RUNNING",
                String.format("Sync started: jobId=%s, dataSource='%s'",
                        job.getId(), source.getName())
        );

        log.info("Sync started: jobId={}, dataSource={}, triggeredBy={}",
                job.getId(), source.getName(), triggeredBy);

        int maxRetries = 3;
        int currentAttempt = 0;
        long currentBackoffMs = 1000;

        while (currentAttempt <= maxRetries) {
            int total = 0, validationPassed = 0, validationFailed = 0,
                processed = 0, failed = 0, skipped = 0;

            try {
                if (currentAttempt > 0) {
                    job.setStatus(SyncJob.SyncStatus.RETRYING);
                    job.setRetryCount(currentAttempt);
                    job.setLastAttemptedAt(LocalDateTime.now());
                    syncRepository.save(job);
                    log.info("Sync retrying (attempt {}): jobId={}", currentAttempt, job.getId());
                }

                // ── Step 3: Fetch pending records ─────────────────────────────────
                List<IngestionRecord> pendingRecords =
                        recordRepository.findPendingSyncRecords(dataSourceId);
                total = pendingRecords.size();

                // ── Step 4: Validate and categorize ──────────────────────────────
                List<UUID> toMarkSynced = new ArrayList<>();

                for (IngestionRecord record : pendingRecords) {
                    SyncRecordValidator.ValidationResult result = validator.validate(record);

                    switch (result.outcome()) {
                        case PASS -> {
                            validationPassed++;
                            toMarkSynced.add(record.getId());
                            processed++;
                        }
                        case FAIL -> {
                            validationFailed++;
                            failed++;
                        }
                        case SKIP -> {
                            skipped++;
                        }
                    }
                }

                // ── Step 5: Bulk-mark validated records as synchronized ───────────
                if (!toMarkSynced.isEmpty()) {
                    List<List<UUID>> batches = partition(toMarkSynced, 500);
                    for (List<UUID> batch : batches) {
                        recordRepository.markAsSynchronized(batch, job.getId());
                    }
                }

                // ── Step 6: Finalize job ──────────────────────────────────────────
                long executionTimeMs = System.currentTimeMillis() - wallStart;
                job.setStatus(SyncJob.SyncStatus.COMPLETED);
                job.setTotalRecords(total);
                job.setValidationPassed(validationPassed);
                job.setValidationFailed(validationFailed);
                job.setRecordsProcessed(processed);
                job.setRecordsFailed(failed);
                job.setRecordsSkipped(skipped);
                job.setCompletedAt(LocalDateTime.now());
                job.setExecutionTimeMs(executionTimeMs);

                SyncJob saved = syncRepository.save(job);

                auditService.log(Constants.ACTION_SYNC_COMPLETED, triggeredBy, "COMPLETED",
                        String.format("Sync completed: jobId=%s, total=%d", saved.getId(), total));
                
                notificationService.createSystemNotification(
                        triggeredBy, 
                        "SUCCESS", 
                        "Sync Completed",
                        String.format("Data source '%s' synced %d records successfully.", source.getName(), total),
                        "SYNC_JOB", 
                        saved.getId()
                );
                
                return java.util.concurrent.CompletableFuture.completedFuture(buildReport(saved));

            } catch (Exception e) {
                // Determine if retryable (e.g., DataAccessException, network issues)
                boolean isRetryable = e instanceof org.springframework.dao.DataAccessException || 
                                      e.getMessage().toLowerCase().contains("timeout") ||
                                      e.getMessage().toLowerCase().contains("connection");
                                      
                if (!isRetryable || currentAttempt == maxRetries) {
                    long executionTimeMs = System.currentTimeMillis() - wallStart;
                    job.setStatus(SyncJob.SyncStatus.FAILED);
                    job.setErrorMessage(truncate(e.getMessage(), 1000));
                    job.setCompletedAt(LocalDateTime.now());
                    job.setExecutionTimeMs(executionTimeMs);
                    SyncJob saved = syncRepository.save(job);

                    auditService.log(Constants.ACTION_SYNC_FAILED, triggeredBy, "FAILED",
                            String.format("Sync failed: jobId=%s, error=%s", saved.getId(), e.getMessage()));

                    notificationService.createSystemNotification(
                            triggeredBy, 
                            "ERROR", 
                            "Sync Failed",
                            String.format("Sync job for '%s' failed: %s", source.getName(), truncate(e.getMessage(), 100)),
                            "SYNC_JOB", 
                            saved.getId()
                    );

                    log.error("Sync failed permanently after {} retries: jobId={}, error={}", 
                              currentAttempt, saved.getId(), e.getMessage());

                    return java.util.concurrent.CompletableFuture.completedFuture(buildReport(saved));
                }

                log.warn("Sync attempt {} failed for jobId={}, retrying in {}ms. Error: {}", 
                         currentAttempt, job.getId(), currentBackoffMs, e.getMessage());
                         
                try {
                    Thread.sleep(currentBackoffMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Retry interrupted", ie);
                }
                
                currentAttempt++;
                currentBackoffMs *= 2;
            }
        }
        
        return java.util.concurrent.CompletableFuture.completedFuture(null);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    private SyncDto.SyncReport buildReport(SyncJob job) {
        boolean fullySuccessful = job.getStatus() == SyncJob.SyncStatus.COMPLETED
                && job.getRecordsFailed() == 0
                && job.getRecordsSkipped() == 0;

        String summary = String.format(
                "Sync %s: %d records synchronized, %d failed validation, %d skipped. "
                + "Execution time: %d ms.",
                job.getStatus().name().toLowerCase(),
                job.getRecordsProcessed(),
                job.getRecordsFailed(),
                job.getRecordsSkipped(),
                job.getExecutionTimeMs() != null ? job.getExecutionTimeMs() : 0L
        );

        String recommendation = null;
        if (job.getRecordsFailed() > 0) {
            recommendation = "Review failed records: ensure all ingestion records have "
                    + "transformed data before the next sync run. "
                    + "Use POST /api/v1/transformation-rules/apply/{jobId} to re-transform.";
        } else if (job.getStatus() == SyncJob.SyncStatus.FAILED) {
            recommendation = "Sync job failed entirely. Check the error message and "
                    + "verify the data source is ACTIVE and reachable.";
        }

        return SyncDto.SyncReport.builder()
                .job(SyncDto.JobResponse.from(job))
                .summary(summary)
                .fullySuccessful(fullySuccessful)
                .recommendation(recommendation)
                .build();
    }

    private <T> List<List<T>> partition(List<T> list, int size) {
        List<List<T>> partitions = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            partitions.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return partitions;
    }

    private String truncate(String value, int maxLength) {
        if (value == null) return null;
        return value.length() <= maxLength ? value : value.substring(0, maxLength - 3) + "...";
    }
}
