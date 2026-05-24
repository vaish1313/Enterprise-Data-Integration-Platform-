package com.company.integrationplatform.dashboard;

import com.company.integrationplatform.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for the Dashboard Analytics module.
 *
 * <p>Provides four aggregated statistics endpoints, each backed by optimized
 * aggregate queries (no full entity loads).
 *
 * <p><b>RBAC:</b> All endpoints are accessible to ADMIN, ANALYST, and OPERATOR.
 * No write operations are exposed here.
 *
 * <p><b>Endpoints:</b>
 * <ul>
 *   <li>GET /api/v1/dashboard/overview        — platform-wide KPI snapshot</li>
 *   <li>GET /api/v1/dashboard/ingestion       — detailed ingestion analytics</li>
 *   <li>GET /api/v1/dashboard/synchronization — detailed sync analytics</li>
 *   <li>GET /api/v1/dashboard/audit           — audit log analytics</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@Tag(
    name = "Dashboard Analytics",
    description = """
        Aggregated statistics APIs for the platform dashboard.

        All endpoints use optimized aggregate queries (COUNT, SUM, AVG, MAX) —
        no full entity collections are loaded into memory.

        **RBAC:** All endpoints are accessible to ADMIN, ANALYST, and OPERATOR.
        """
)
@SecurityRequirement(name = "bearerAuth")
public class DashboardController {

    private final DashboardService dashboardService;

    // ─────────────────────────────────────────────────────────────────────────
    // OVERVIEW
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns a platform-wide KPI snapshot.
     *
     * <p>Covers: users (total, active, by role), data sources (total, active, inactive, error),
     * ingestion (total jobs, successful, failed, imported records), transformation rules,
     * and synchronization (total jobs, successful, failed, last sync time, success rate).
     *
     * <p><b>Allowed roles:</b> ADMIN, ANALYST, OPERATOR
     */
    @GetMapping("/overview")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST', 'OPERATOR')")
    @Operation(
        summary = "Get platform-wide KPI overview",
        description = """
            Returns a single-response KPI snapshot covering every major module:

            - **Users:** total, active, breakdown by role (ADMIN / ANALYST / OPERATOR)
            - **Data Sources:** total, active, inactive, error
            - **Ingestion:** total jobs, successful, failed, total imported records
            - **Transformation:** total rules, active rules
            - **Synchronization:** total jobs, successful, failed, last sync time,
              sync success %, ingestion success %

            All values are computed via aggregate queries — no entity collections loaded.

            **Allowed roles: ADMIN, ANALYST, OPERATOR**
            """
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Overview statistics retrieved successfully",
            content = @Content(schema = @Schema(implementation = DashboardDto.OverviewStats.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "Insufficient permissions"
        )
    })
    public ResponseEntity<ApiResponse<DashboardDto.OverviewStats>> getOverview() {
        return ResponseEntity.ok(
                ApiResponse.success("Dashboard overview retrieved", dashboardService.getOverview()));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // INGESTION
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns detailed ingestion analytics.
     *
     * <p><b>Allowed roles:</b> ADMIN, ANALYST, OPERATOR
     */
    @GetMapping("/ingestion")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST', 'OPERATOR')")
    @Operation(
        summary = "Get detailed ingestion analytics",
        description = """
            Returns detailed ingestion statistics:

            - **Job counts:** total, completed, failed, running, pending, partial
            - **Record counts:** total, processed, failed, pending sync, already synchronized
            - **Rates:** job success %, average records per completed job
            - **Type breakdown:** CSV jobs, REST API jobs, scheduled jobs

            **Allowed roles: ADMIN, ANALYST, OPERATOR**
            """
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Ingestion statistics retrieved successfully",
            content = @Content(schema = @Schema(implementation = DashboardDto.IngestionStats.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "Insufficient permissions"
        )
    })
    public ResponseEntity<ApiResponse<DashboardDto.IngestionStats>> getIngestion() {
        return ResponseEntity.ok(
                ApiResponse.success("Ingestion statistics retrieved",
                        dashboardService.getIngestionStats()));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SYNCHRONIZATION
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns detailed synchronization analytics.
     *
     * <p><b>Allowed roles:</b> ADMIN, ANALYST, OPERATOR
     */
    @GetMapping("/synchronization")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST', 'OPERATOR')")
    @Operation(
        summary = "Get detailed synchronization analytics",
        description = """
            Returns detailed synchronization statistics:

            - **Job counts:** total, completed, failed, running, pending
            - **Record counts:** total synchronized, total failed, pending sync
            - **Timing:** last successful sync timestamp, average execution time (ms),
              maximum execution time (ms)
            - **Rate:** sync job success %

            **Allowed roles: ADMIN, ANALYST, OPERATOR**
            """
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Synchronization statistics retrieved successfully",
            content = @Content(schema = @Schema(implementation = DashboardDto.SynchronizationStats.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "Insufficient permissions"
        )
    })
    public ResponseEntity<ApiResponse<DashboardDto.SynchronizationStats>> getSynchronization() {
        return ResponseEntity.ok(
                ApiResponse.success("Synchronization statistics retrieved",
                        dashboardService.getSyncStats()));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // AUDIT
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns audit log analytics.
     *
     * <p><b>Allowed roles:</b> ADMIN, ANALYST, OPERATOR
     */
    @GetMapping("/audit")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST', 'OPERATOR')")
    @Operation(
        summary = "Get audit log analytics",
        description = """
            Returns audit log statistics:

            - **Counts:** total events, successful events, failed events
            - **Users:** distinct usernames that appear in the audit log
            - **Rate:** event success %
            - **Breakdown:** event counts grouped by action type (sorted by count desc)
            - **Recent events:** the 10 most recent audit log entries

            **Allowed roles: ADMIN, ANALYST, OPERATOR**
            """
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Audit statistics retrieved successfully",
            content = @Content(schema = @Schema(implementation = DashboardDto.AuditStats.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "Insufficient permissions"
        )
    })
    public ResponseEntity<ApiResponse<DashboardDto.AuditStats>> getAudit() {
        return ResponseEntity.ok(
                ApiResponse.success("Audit statistics retrieved",
                        dashboardService.getAuditStats()));
    }
}
