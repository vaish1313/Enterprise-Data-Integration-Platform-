package com.company.integrationplatform.ingestion;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

public class IngestionDto {

    @Getter
    @Builder
    public static class JobResponse {

        private UUID id;
        private UUID dataSourceId;
        private IngestionJob.JobStatus status;
        private IngestionJob.IngestionType ingestionType;
        private LocalDateTime startedAt;
        private LocalDateTime completedAt;
        private int recordsProcessed;
        private int recordsFailed;
        private String errorMessage;
        private String triggeredBy;
        private LocalDateTime createdAt;

        public static JobResponse from(IngestionJob job) {
            return JobResponse.builder()
                    .id(job.getId())
                    .dataSourceId(job.getDataSourceId())
                    .status(job.getStatus())
                    .ingestionType(job.getIngestionType())
                    .startedAt(job.getStartedAt())
                    .completedAt(job.getCompletedAt())
                    .recordsProcessed(job.getRecordsProcessed())
                    .recordsFailed(job.getRecordsFailed())
                    .errorMessage(job.getErrorMessage())
                    .triggeredBy(job.getTriggeredBy())
                    .createdAt(job.getCreatedAt())
                    .build();
        }
    }

    @Getter
    @Builder
    public static class ApiIngestionRequest {
        private UUID dataSourceId;
        private String endpointOverride;
    }
}
