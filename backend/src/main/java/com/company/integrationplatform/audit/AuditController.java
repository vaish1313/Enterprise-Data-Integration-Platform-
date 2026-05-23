package com.company.integrationplatform.audit;

import com.company.integrationplatform.common.ApiResponse;
import com.company.integrationplatform.common.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * REST controller for the Audit Log module.
 *
 * <p><b>RBAC Summary:</b>
 * <ul>
 *   <li>READ  (view / filter logs) — ADMIN, ANALYST, OPERATOR</li>
 *   <li>EXPORT (CSV download)      — ADMIN, ANALYST</li>
 *   <li>DELETE (purge logs)        — ADMIN only</li>
 * </ul>
 *
 * <p>Authorization is enforced at <b>method level</b> via {@code @PreAuthorize}.
 * No class-level annotation is used so each endpoint's access policy is
 * explicit, self-documenting, and independently adjustable.
 *
 * <p>Spring Security's {@code @EnableMethodSecurity} (declared in SecurityConfig)
 * must be active for {@code @PreAuthorize} to take effect.
 */
@RestController
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
@Tag(name = "Audit Logs", description = "APIs for querying, exporting, and managing platform audit logs. "
        + "Access is role-restricted: READ=ADMIN/ANALYST/OPERATOR, EXPORT=ADMIN/ANALYST, DELETE=ADMIN only.")
@SecurityRequirement(name = "bearerAuth")
public class AuditController {

    private final AuditService auditService;

    // ─────────────────────────────────────────────────────────────────────────
    // READ ENDPOINTS  —  ADMIN | ANALYST | OPERATOR
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns a paginated list of all audit log entries, sorted by most recent first.
     *
     * <p><b>Allowed roles:</b> ADMIN, ANALYST, OPERATOR
     * <p><b>Security rationale:</b> All authenticated roles need visibility into
     * platform activity for operational awareness. No sensitive mutation is possible
     * through this read-only endpoint.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST', 'OPERATOR')")
    @Operation(
        summary = "Get all audit logs (paginated)",
        description = "Returns paginated audit log entries sorted by timestamp descending. "
                    + "**Allowed roles: ADMIN, ANALYST, OPERATOR**"
    )
    public ResponseEntity<ApiResponse<PageResponse<AuditDto>>> getAll(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "50") int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        return ResponseEntity.ok(ApiResponse.success(auditService.getAll(pageable)));
    }

    /**
     * Returns audit log entries filtered by a specific username.
     *
     * <p><b>Allowed roles:</b> ADMIN, ANALYST, OPERATOR
     * <p><b>Security rationale:</b> Operators may need to review their own or
     * their team's activity. Filtering by username does not expose more data
     * than the full log view already permits.
     */
    @GetMapping("/user/{username}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST', 'OPERATOR')")
    @Operation(
        summary = "Get audit logs by username",
        description = "Returns audit log entries for a specific user. "
                    + "**Allowed roles: ADMIN, ANALYST, OPERATOR**"
    )
    public ResponseEntity<ApiResponse<PageResponse<AuditDto>>> getByUser(
            @PathVariable String username,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "50") int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        return ResponseEntity.ok(ApiResponse.success(auditService.getByUser(username, pageable)));
    }

    /**
     * Returns audit log entries filtered by action type (e.g. USER_LOGIN, INGEST_CSV).
     *
     * <p><b>Allowed roles:</b> ADMIN, ANALYST, OPERATOR
     * <p><b>Security rationale:</b> Action-based filtering is a standard operational
     * query. No write or delete capability is exposed.
     */
    @GetMapping("/action/{action}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST', 'OPERATOR')")
    @Operation(
        summary = "Get audit logs by action type",
        description = "Returns audit log entries matching a specific action (e.g. USER_LOGIN, INGEST_CSV). "
                    + "**Allowed roles: ADMIN, ANALYST, OPERATOR**"
    )
    public ResponseEntity<ApiResponse<PageResponse<AuditDto>>> getByAction(
            @PathVariable String action,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "50") int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        return ResponseEntity.ok(ApiResponse.success(auditService.getByAction(action, pageable)));
    }

    /**
     * Returns audit log entries within a specified date/time range.
     *
     * <p><b>Allowed roles:</b> ADMIN, ANALYST, OPERATOR
     * <p><b>Security rationale:</b> Time-range queries support incident investigation
     * and operational monitoring. Read-only; no data is modified.
     */
    @GetMapping("/range")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST', 'OPERATOR')")
    @Operation(
        summary = "Get audit logs within a date range",
        description = "Returns audit log entries between `from` and `to` timestamps (ISO-8601). "
                    + "**Allowed roles: ADMIN, ANALYST, OPERATOR**"
    )
    public ResponseEntity<ApiResponse<PageResponse<AuditDto>>> getByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "50") int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        return ResponseEntity.ok(ApiResponse.success(auditService.getByDateRange(from, to, pageable)));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // EXPORT ENDPOINTS  —  ADMIN | ANALYST  (OPERATOR denied)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Exports all audit logs as a CSV file download.
     *
     * <p><b>Allowed roles:</b> ADMIN, ANALYST
     * <p><b>Denied roles:</b> OPERATOR
     * <p><b>Security rationale:</b> Bulk export of audit data could expose sensitive
     * operational intelligence. Restricted to privileged roles who have a legitimate
     * compliance or reporting need. OPERATORs can view logs in the UI but cannot
     * extract them in bulk.
     */
    @GetMapping("/export/csv")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    @Operation(
        summary = "Export audit logs as CSV",
        description = "Downloads all audit log entries as a CSV file. "
                    + "**Allowed roles: ADMIN, ANALYST** — OPERATOR is denied."
    )
    public void exportCsv(HttpServletResponse response) throws IOException {
        response.setContentType(MediaType.TEXT_PLAIN_VALUE);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"audit-logs-" + LocalDateTime.now().toLocalDate() + ".csv\"");
        auditService.exportToCsv(response.getWriter());
    }

    /**
     * Exports audit logs filtered by date range as a CSV file download.
     *
     * <p><b>Allowed roles:</b> ADMIN, ANALYST
     * <p><b>Denied roles:</b> OPERATOR
     * <p><b>Security rationale:</b> Same as full export — bulk data extraction
     * is restricted to privileged roles only.
     */
    @GetMapping("/export/csv/range")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    @Operation(
        summary = "Export audit logs for a date range as CSV",
        description = "Downloads audit log entries between `from` and `to` as a CSV file. "
                    + "**Allowed roles: ADMIN, ANALYST** — OPERATOR is denied."
    )
    public void exportCsvByRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            HttpServletResponse response) throws IOException {
        response.setContentType(MediaType.TEXT_PLAIN_VALUE);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"audit-logs-range-" + LocalDateTime.now().toLocalDate() + ".csv\"");
        auditService.exportRangeToCsv(from, to, response.getWriter());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DELETE ENDPOINTS  —  ADMIN only
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Permanently deletes a single audit log entry by its ID.
     *
     * <p><b>Allowed roles:</b> ADMIN
     * <p><b>Denied roles:</b> ANALYST, OPERATOR
     * <p><b>Security rationale:</b> Deleting audit records is a destructive,
     * irreversible operation that could compromise compliance and forensic integrity.
     * Restricted to ADMIN only to maintain a tamper-evident audit trail.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Delete a single audit log entry",
        description = "Permanently deletes an audit log entry by ID. This action is irreversible. "
                    + "**Allowed roles: ADMIN only** — ANALYST and OPERATOR are denied."
    )
    public ResponseEntity<ApiResponse<Void>> deleteById(@PathVariable UUID id) {
        auditService.deleteById(id);
        return ResponseEntity.ok(ApiResponse.success("Audit log entry deleted", null));
    }

    /**
     * Purges all audit log entries older than the specified number of days.
     *
     * <p><b>Allowed roles:</b> ADMIN
     * <p><b>Denied roles:</b> ANALYST, OPERATOR
     * <p><b>Security rationale:</b> Bulk purge is a high-impact, irreversible
     * operation. Only ADMIN may perform data retention management to prevent
     * accidental or malicious destruction of the audit trail.
     */
    @DeleteMapping("/purge")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Purge audit logs older than N days",
        description = "Permanently deletes all audit log entries older than the specified number of days. "
                    + "**Allowed roles: ADMIN only** — ANALYST and OPERATOR are denied."
    )
    public ResponseEntity<ApiResponse<Void>> purgeOlderThan(
            @RequestParam(defaultValue = "90") int days) {
        long deleted = auditService.purgeOlderThan(days);
        return ResponseEntity.ok(ApiResponse.success(
                String.format("Purged %d audit log entries older than %d days", deleted, days), null));
    }
}
