package com.company.integrationplatform.synchronization;

import com.company.integrationplatform.common.ApiResponse;
import com.company.integrationplatform.common.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for the Synchronization Engine.
 *
 * <p><b>RBAC Summary:</b>
 * <ul>
 *   <li>RUN SYNC  — ADMIN, ANALYST</li>
 *   <li>VIEW JOBS — ADMIN, ANALYST, OPERATOR</li>
 *   <li>STATISTICS — ADMIN, ANALYST, OPERATOR</li>
 * </ul>
 *
 * <p>Authorization is enforced at method level via {@code @PreAuthorize}.
 */
@RestController
@RequestMapping("/api/v1/sync")
@RequiredArgsConstructor
@Tag(
    name = "Synchronization Engine",
    description = """
        APIs for running and monitoring synchronization jobs.

        The sync engine fetches all PROCESSED, un-synchronized ingestion records
        for a data source, validates them, marks them as synchronized, and
        generates a completion report.

        **Scheduler:** Runs automatically every 5 minutes for all ACTIVE data sources.

        **RBAC:** RUN=ADMIN/ANALYST, VIEW/STATISTICS=ADMIN/ANALYST/OPERATOR
        """
)
@SecurityRequirement(name = "bearerAuth")
public class SyncController {

    private final SyncService syncService;

    // ─────────────────────────────────────────────────────────────────────────
    // RUN SYNC
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Manually triggers a synchronization run for a specific data source.
     *
     * <p><b>Allowed roles:</b> ADMIN, ANALYST
     * <p><b>Security rationale:</b> Triggering a sync moves data to target systems.
     * Restricted to privileged roles. OPERATOR can monitor but not trigger.
     */
    @PostMapping("/run")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    @Operation(
        summary = "Manually trigger a synchronization run",
        description = """
            Triggers an immediate synchronization run for the specified data source.

            **Workflow:**
            1. Fetch all PROCESSED, un-synchronized ingestion records
            2. Validate each record (transformed data present, status PROCESSED)
            3. Mark validated records as synchronized
            4. Generate a completion report with counts and execution time

            **Audit events:** `SYNC_STARTED` → `SYNC_COMPLETED` | `SYNC_FAILED`

            **Allowed roles: ADMIN, ANALYST** — OPERATOR is denied.
            """
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Sync completed (check report.fullySuccessful for outcome)",
            content = @Content(schema = @Schema(implementation = SyncDto.SyncReport.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "Insufficient permissions — OPERATOR cannot trigger sync"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Data source not found"
        )
    })
    public ResponseEntity<ApiResponse<SyncDto.SyncReport>> runSync(
            @Valid @RequestBody SyncDto.RunRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Sync completed",
                syncService.runSync(request.getDataSourceId())));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // LIST ALL JOBS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns a paginated list of all sync jobs, newest first.
     *
     * <p><b>Allowed roles:</b> ADMIN, ANALYST, OPERATOR
     */
    @GetMapping("/jobs")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST', 'OPERATOR')")
    @Operation(
        summary = "List all sync jobs (paginated)",
        description = "Returns a paginated list of all synchronization jobs sorted by start time. "
                    + "**Allowed roles: ADMIN, ANALYST, OPERATOR**"
    )
    public ResponseEntity<ApiResponse<PageResponse<SyncDto.JobResponse>>> getAllJobs(
            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Page size", example = "20")
            @RequestParam(defaultValue = "20") int size,

            @Parameter(description = "Sort direction: asc | desc", example = "desc")
            @RequestParam(defaultValue = "desc") String direction) {

        Sort sort = "asc".equalsIgnoreCase(direction)
                ? Sort.by("startedAt").ascending()
                : Sort.by("startedAt").descending();

        return ResponseEntity.ok(ApiResponse.success(
                syncService.getAllJobs(PageRequest.of(page, size, sort))));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET JOB BY ID
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns full details of a single sync job.
     *
     * <p><b>Allowed roles:</b> ADMIN, ANALYST, OPERATOR
     */
    @GetMapping("/jobs/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST', 'OPERATOR')")
    @Operation(
        summary = "Get sync job by ID",
        description = "Returns full details of a single sync job including record counts, "
                    + "execution time, and validation results. "
                    + "**Allowed roles: ADMIN, ANALYST, OPERATOR**"
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Sync job found",
            content = @Content(schema = @Schema(implementation = SyncDto.JobResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Sync job not found"
        )
    })
    public ResponseEntity<ApiResponse<SyncDto.JobResponse>> getJobById(
            @Parameter(description = "Sync job UUID", required = true)
            @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(syncService.getJobById(id)));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET JOBS BY DATA SOURCE
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns paginated sync jobs for a specific data source.
     *
     * <p><b>Allowed roles:</b> ADMIN, ANALYST, OPERATOR
     */
    @GetMapping("/jobs/source/{dataSourceId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST', 'OPERATOR')")
    @Operation(
        summary = "Get sync jobs for a data source",
        description = "Returns paginated sync jobs for a specific data source. "
                    + "**Allowed roles: ADMIN, ANALYST, OPERATOR**"
    )
    public ResponseEntity<ApiResponse<PageResponse<SyncDto.JobResponse>>> getJobsBySource(
            @Parameter(description = "Data source UUID", required = true)
            @PathVariable UUID dataSourceId,

            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "desc") String direction) {

        Sort sort = "asc".equalsIgnoreCase(direction)
                ? Sort.by("startedAt").ascending()
                : Sort.by("startedAt").descending();

        return ResponseEntity.ok(ApiResponse.success(
                syncService.getJobsBySource(dataSourceId, PageRequest.of(page, size, sort))));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RECENT JOBS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns the N most recent sync jobs across all data sources.
     *
     * <p><b>Allowed roles:</b> ADMIN, ANALYST, OPERATOR
     */
    @GetMapping("/jobs/recent")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST', 'OPERATOR')")
    @Operation(
        summary = "Get recent sync jobs",
        description = "Returns the most recent sync jobs across all data sources. "
                    + "**Allowed roles: ADMIN, ANALYST, OPERATOR**"
    )
    public ResponseEntity<ApiResponse<List<SyncDto.JobResponse>>> getRecentJobs(
            @Parameter(description = "Maximum number of jobs to return", example = "10")
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(ApiResponse.success(syncService.getRecentJobs(limit)));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // STATISTICS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns platform-wide synchronization statistics.
     *
     * <p><b>Allowed roles:</b> ADMIN, ANALYST, OPERATOR
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST', 'OPERATOR')")
    @Operation(
        summary = "Get synchronization statistics",
        description = """
            Returns platform-wide synchronization statistics including:
            - Job counts by status (COMPLETED, FAILED, RUNNING, PENDING)
            - Total records synchronized and failed
            - Records pending synchronization right now
            - Average and maximum execution times
            - Last successful sync timestamp
            - Overall success rate percentage

            **Allowed roles: ADMIN, ANALYST, OPERATOR**
            """
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Statistics retrieved successfully",
            content = @Content(schema = @Schema(implementation = SyncDto.StatisticsResponse.class))
        )
    })
    public ResponseEntity<ApiResponse<SyncDto.StatisticsResponse>> getStatistics() {
        return ResponseEntity.ok(ApiResponse.success(syncService.getStatistics()));
    }
}
