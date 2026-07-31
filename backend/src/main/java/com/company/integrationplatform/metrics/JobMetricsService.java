package com.company.integrationplatform.metrics;

import com.company.integrationplatform.ingestion.IngestionJob;
import com.company.integrationplatform.ingestion.IngestionRepository;
import com.company.integrationplatform.synchronization.SyncJob;
import com.company.integrationplatform.synchronization.SyncRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class JobMetricsService {

    private final IngestionRepository ingestionJobRepository;
    private final SyncRepository syncRepository;

    @Transactional(readOnly = true)
    public JobMetricsController.JobMetricsResponse getMetrics() {
        long totalIngest = ingestionJobRepository.count();
        long totalSync = syncRepository.count();
        long totalJobs = totalIngest + totalSync;

        long successIngest = ingestionJobRepository.countByStatus(IngestionJob.JobStatus.COMPLETED);
        long successSync = syncRepository.countByStatus(SyncJob.SyncStatus.COMPLETED);
        long totalSuccess = successIngest + successSync;

        long retryingIngest = ingestionJobRepository.countByStatus(IngestionJob.JobStatus.RETRYING);
        long retryingSync = syncRepository.countByStatus(SyncJob.SyncStatus.RETRYING);

        double successRate = totalJobs > 0 ? (double) totalSuccess / totalJobs * 100.0 : 0.0;

        // Native queries or manual aggregation for advanced stats
        Double avgRetryIngest = ingestionJobRepository.getAverageRetryCount();
        Double avgRetrySync = syncRepository.getAverageRetryCount();
        double totalAvgRetry = 0.0;
        if (avgRetryIngest != null && avgRetrySync != null) {
            totalAvgRetry = (avgRetryIngest + avgRetrySync) / 2.0;
        } else if (avgRetryIngest != null) {
            totalAvgRetry = avgRetryIngest;
        } else if (avgRetrySync != null) {
            totalAvgRetry = avgRetrySync;
        }

        Double avgTimeIngest = ingestionJobRepository.getAverageProcessingTimeMs();
        Double avgTimeSync = syncRepository.avgExecutionTimeMs();
        long avgTime = 0;
        if (avgTimeIngest != null && avgTimeSync != null) {
            avgTime = (long) ((avgTimeIngest + avgTimeSync) / 2);
        } else if (avgTimeIngest != null) {
            avgTime = avgTimeIngest.longValue();
        } else if (avgTimeSync != null) {
            avgTime = avgTimeSync.longValue();
        }

        return JobMetricsController.JobMetricsResponse.builder()
                .totalJobsProcessed(totalJobs)
                .successRatePercent(Math.round(successRate * 10.0) / 10.0)
                .averageRetryCount(Math.round(totalAvgRetry * 10.0) / 10.0)
                .averageProcessingTimeMs(avgTime)
                .jobsCurrentlyRetrying(retryingIngest + retryingSync)
                .build();
    }
}
