package com.company.integrationplatform.dashboard;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class DashboardDto {

    private long totalImports;
    private long successfulImports;
    private long failedImports;
    private long partialImports;
    private long activeDataSources;
    private long totalDataSources;
    private long totalRecordsProcessed;
    private long totalRecordsFailed;
    private LocalDateTime lastSynchronizationTime;
    private long totalTransformationRules;
    private long totalUsers;
}
