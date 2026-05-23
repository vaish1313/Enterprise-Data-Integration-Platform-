package com.company.integrationplatform.synchronization;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

public class SyncDto {

    @Getter
    @Builder
    public static class Response {

        private UUID id;
        private UUID dataSourceId;
        private SyncJob.SyncStatus status;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private int recordsProcessed;
        private int recordsFailed;
        private String errorMessage;
        private String triggeredBy;
        private LocalDateTime createdAt;

        public static Response from(SyncJob job) {
            return Response.builder()
                    .id(job.getId())
                    .dataSourceId(job.getDataSourceId())
                    .status(job.getStatus())
                    .startTime(job.getStartTime())
                    .endTime(job.getEndTime())
                    .recordsProcessed(job.getRecordsProcessed())
                    .recordsFailed(job.getRecordsFailed())
                    .errorMessage(job.getErrorMessage())
                    .triggeredBy(job.getTriggeredBy())
                    .createdAt(job.getCreatedAt())
                    .build();
        }
    }
}
