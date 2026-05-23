package com.company.integrationplatform.dashboard;

import com.company.integrationplatform.datasource.DataSourceEntity;
import com.company.integrationplatform.datasource.DataSourceRepository;
import com.company.integrationplatform.ingestion.IngestionJob;
import com.company.integrationplatform.ingestion.IngestionRepository;
import com.company.integrationplatform.synchronization.SyncJob;
import com.company.integrationplatform.synchronization.SyncRepository;
import com.company.integrationplatform.transformation.TransformationRepository;
import com.company.integrationplatform.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final IngestionRepository ingestionRepository;
    private final DataSourceRepository dataSourceRepository;
    private final SyncRepository syncRepository;
    private final TransformationRepository transformationRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public DashboardDto getSummary() {
        long totalImports       = ingestionRepository.count();
        long successfulImports  = ingestionRepository.countByStatus(IngestionJob.JobStatus.COMPLETED);
        long failedImports      = ingestionRepository.countByStatus(IngestionJob.JobStatus.FAILED);
        long partialImports     = ingestionRepository.countByStatus(IngestionJob.JobStatus.PARTIAL);
        long activeDataSources  = dataSourceRepository.countByStatus(DataSourceEntity.SourceStatus.ACTIVE);
        long totalDataSources   = dataSourceRepository.count();
        long totalRules         = transformationRepository.count();
        long totalUsers         = userRepository.count();

        Long recordsProcessed = ingestionRepository.sumSuccessfulRecords();
        Long recordsFailed    = ingestionRepository.sumFailedRecords();

        List<SyncJob> lastSync = syncRepository.findLatestJobs(PageRequest.of(0, 1));
        java.time.LocalDateTime lastSyncTime = lastSync.isEmpty()
                ? null : lastSync.get(0).getEndTime();

        return DashboardDto.builder()
                .totalImports(totalImports)
                .successfulImports(successfulImports)
                .failedImports(failedImports)
                .partialImports(partialImports)
                .activeDataSources(activeDataSources)
                .totalDataSources(totalDataSources)
                .totalRecordsProcessed(recordsProcessed != null ? recordsProcessed : 0L)
                .totalRecordsFailed(recordsFailed != null ? recordsFailed : 0L)
                .lastSynchronizationTime(lastSyncTime)
                .totalTransformationRules(totalRules)
                .totalUsers(totalUsers)
                .build();
    }
}
