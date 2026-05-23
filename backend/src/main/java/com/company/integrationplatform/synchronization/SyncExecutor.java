package com.company.integrationplatform.synchronization;

import com.company.integrationplatform.audit.AuditService;
import com.company.integrationplatform.common.Constants;
import com.company.integrationplatform.ingestion.IngestionDto;
import com.company.integrationplatform.ingestion.IngestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Extracted into a separate Spring bean so that @Transactional is applied
 * via the Spring proxy — avoids the self-invocation proxy bypass problem.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SyncExecutor {

    private final SyncRepository syncRepository;
    private final IngestionService ingestionService;
    private final AuditService auditService;

    @Transactional
    public SyncDto.Response execute(UUID dataSourceId, String triggeredBy) {
        SyncJob job = SyncJob.builder()
                .dataSourceId(dataSourceId)
                .status(SyncJob.SyncStatus.RUNNING)
                .startTime(LocalDateTime.now())
                .triggeredBy(triggeredBy)
                .build();
        job = syncRepository.save(job);

        try {
            IngestionDto.JobResponse ingestionResult = ingestionService.ingestFromApi(dataSourceId);

            job.setRecordsProcessed(ingestionResult.getRecordsProcessed());
            job.setRecordsFailed(ingestionResult.getRecordsFailed());
            job.setStatus(ingestionResult.getRecordsFailed() == 0
                    ? SyncJob.SyncStatus.COMPLETED
                    : SyncJob.SyncStatus.PARTIAL);

        } catch (Exception e) {
            log.error("Sync failed for data source {}: {}", dataSourceId, e.getMessage());
            job.setStatus(SyncJob.SyncStatus.FAILED);
            job.setErrorMessage(e.getMessage());
        }

        job.setEndTime(LocalDateTime.now());
        SyncJob saved = syncRepository.save(job);

        auditService.log(Constants.ACTION_SYNC, triggeredBy, saved.getStatus().name(),
                String.format("Sync for source %s: %d processed, %d failed",
                        dataSourceId, saved.getRecordsProcessed(), saved.getRecordsFailed()));

        return SyncDto.Response.from(saved);
    }
}
