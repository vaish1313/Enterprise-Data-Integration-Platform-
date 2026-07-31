package com.company.integrationplatform.metrics;

import com.company.integrationplatform.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
@Tag(name = "Job Metrics", description = "Aggregated metrics for background jobs")
public class JobMetricsController {

    private final JobMetricsService jobMetricsService;

    @GetMapping("/metrics")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST', 'OPERATOR')")
    @Operation(summary = "Get aggregated job metrics")
    public ResponseEntity<ApiResponse<JobMetricsResponse>> getMetrics() {
        return ResponseEntity.ok(ApiResponse.success("Job metrics retrieved", jobMetricsService.getMetrics()));
    }

    @Data
    @Builder
    public static class JobMetricsResponse {
        private long totalJobsProcessed;
        private double successRatePercent;
        private double averageRetryCount;
        private long averageProcessingTimeMs;
        private long jobsCurrentlyRetrying;
    }
}
