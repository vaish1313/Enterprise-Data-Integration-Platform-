package com.company.integrationplatform.synchronization;

import com.company.integrationplatform.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sync")
@RequiredArgsConstructor
@Tag(name = "Synchronization", description = "APIs for managing and monitoring synchronization jobs")
public class SyncController {

    private final SyncService syncService;

    @PostMapping("/trigger/{dataSourceId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    @Operation(summary = "Manually trigger synchronization for a data source")
    public ResponseEntity<ApiResponse<SyncDto.Response>> triggerSync(
            @PathVariable UUID dataSourceId) {
        return ResponseEntity.ok(ApiResponse.success("Sync triggered",
                syncService.triggerManualSync(dataSourceId)));
    }

    @GetMapping("/jobs/{id}")
    @Operation(summary = "Get sync job by ID")
    public ResponseEntity<ApiResponse<SyncDto.Response>> getJob(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(syncService.getJobById(id)));
    }

    @GetMapping("/jobs/recent")
    @Operation(summary = "Get recent sync jobs")
    public ResponseEntity<ApiResponse<List<SyncDto.Response>>> getRecent(
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(ApiResponse.success(syncService.getRecentJobs(limit)));
    }

    @GetMapping("/jobs/source/{dataSourceId}")
    @Operation(summary = "Get sync jobs for a specific data source")
    public ResponseEntity<ApiResponse<List<SyncDto.Response>>> getBySource(
            @PathVariable UUID dataSourceId) {
        return ResponseEntity.ok(ApiResponse.success(syncService.getJobsBySource(dataSourceId)));
    }
}
