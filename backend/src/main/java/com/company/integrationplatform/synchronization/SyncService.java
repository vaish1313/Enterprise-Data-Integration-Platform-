package com.company.integrationplatform.synchronization;

import com.company.integrationplatform.datasource.DataSourceEntity;
import com.company.integrationplatform.datasource.DataSourceRepository;
import com.company.integrationplatform.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SyncService {

    private final SyncRepository syncRepository;
    private final DataSourceRepository dataSourceRepository;
    private final SyncExecutor syncExecutor;

    /**
     * Scheduled sync — runs every 5 minutes.
     * Delegates to SyncExecutor (a separate Spring bean) so @Transactional
     * is properly applied via the proxy for each source sync.
     */
    @Scheduled(fixedDelayString = "${sync.interval-ms:300000}")
    public void runScheduledSync() {
        log.info("Starting scheduled synchronization at {}", LocalDateTime.now());

        List<DataSourceEntity> activeSources =
                dataSourceRepository.findByStatus(DataSourceEntity.SourceStatus.ACTIVE);

        for (DataSourceEntity source : activeSources) {
            if (source.getSourceType() == DataSourceEntity.SourceType.REST_API) {
                try {
                    syncExecutor.execute(source.getId(), "SCHEDULER");
                } catch (Exception e) {
                    // Log and continue — one failing source must not stop others
                    log.error("Scheduled sync failed for source '{}': {}",
                            source.getName(), e.getMessage());
                }
            }
        }

        log.info("Scheduled synchronization completed at {}", LocalDateTime.now());
    }

    /**
     * Manually trigger synchronization for a specific data source.
     */
    public SyncDto.Response triggerManualSync(UUID dataSourceId) {
        return syncExecutor.execute(dataSourceId, "MANUAL");
    }

    @Transactional(readOnly = true)
    public SyncDto.Response getJobById(UUID id) {
        return SyncDto.Response.from(syncRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SyncJob", id)));
    }

    @Transactional(readOnly = true)
    public List<SyncDto.Response> getRecentJobs(int limit) {
        return syncRepository.findLatestJobs(PageRequest.of(0, limit))
                .stream().map(SyncDto.Response::from).toList();
    }

    @Transactional(readOnly = true)
    public List<SyncDto.Response> getJobsBySource(UUID dataSourceId) {
        return syncRepository.findByDataSourceId(dataSourceId)
                .stream().map(SyncDto.Response::from).toList();
    }
}
