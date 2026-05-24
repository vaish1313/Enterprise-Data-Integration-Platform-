package com.company.integrationplatform.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTO container for the Dashboard Analytics module.
 *
 * <p>Each nested class maps to one dashboard endpoint:
 * <ul>
 *   <li>{@link OverviewStats}       — GET /api/v1/dashboard/overview</li>
 *   <li>{@link IngestionStats}      — GET /api/v1/dashboard/ingestion</li>
 *   <li>{@link SynchronizationStats}— GET /api/v1/dashboard/synchronization</li>
 *   <li>{@link AuditStats}          — GET /api/v1/dashboard/audit</li>
 * </ul>
 */
public class DashboardDto {

    // ─────────────────────────────────────────────────────────────────────────
    // OVERVIEW  —  platform-wide KPIs
    // ─────────────────────────────────────────────────────────────────────────

    @Getter
    @Builder
    @Schema(
        name = "DashboardOverviewStats",
        description = "Platform-wide KPI snapshot covering users, data sources, "
                    + "ingestion, transformation, and synchronization totals"
    )
    public static class OverviewStats {

        // ── Users ─────────────────────────────────────────────────────────────

        @Schema(description = "Total registered users", example = "42")
        private long totalUsers;

        @Schema(description = "Active (enabled) users", example = "38")
        private long activeUsers;

        @Schema(description = "Users with ADMIN role", example = "5")
        private long adminUsers;

        @Schema(description = "Users with ANALYST role", example = "20")
        private long analystUsers;

        @Schema(description = "Users with OPERATOR role", example = "17")
        private long operatorUsers;

        // ── Data Sources ──────────────────────────────────────────────────────

        @Schema(description = "Total registered data sources", example = "15")
        private long totalDataSources;

        @Schema(description = "Data sources with status ACTIVE", example = "10")
        private long activeDataSources;

        @Schema(description = "Data sources with status INACTIVE", example = "4")
        private long inactiveDataSources;

        @Schema(description = "Data sources with status ERROR", example = "1")
        private long errorDataSources;

        // ── Ingestion ─────────────────────────────────────────────────────────

        @Schema(description = "Total ingestion jobs ever run", example = "320")
        private long totalIngestionJobs;

        @Schema(description = "Ingestion jobs with status COMPLETED", example = "298")
        private long successfulIngestionJobs;

        @Schema(description = "Ingestion jobs with status FAILED", example = "14")
        private long failedIngestionJobs;

        @Schema(description = "Total records imported across all completed jobs", example = "1250000")
        private long totalImportedRecords;

        // ── Transformation ────────────────────────────────────────────────────

        @Schema(description = "Total transformation rules defined", example = "47")
        private long totalTransformationRules;

        @Schema(description = "Active transformation rules", example = "43")
        private long activeTransformationRules;

        // ── Synchronization ───────────────────────────────────────────────────

        @Schema(description = "Total synchronization jobs ever run", example = "180")
        private long totalSyncJobs;

        @Schema(description = "Synchronization jobs with status COMPLETED", example = "172")
        private long successfulSyncJobs;

        @Schema(description = "Synchronization jobs with status FAILED", example = "8")
        private long failedSyncJobs;

        @Schema(
            description = "Timestamp of the last completed synchronization",
            example = "2026-05-23T10:15:45"
        )
        private LocalDateTime lastSynchronizationTime;

        @Schema(
            description = "Overall sync success rate as a percentage (0–100)",
            example = "95.6"
        )
        private double syncSuccessPercent;

        @Schema(
            description = "Overall ingestion success rate as a percentage (0–100)",
            example = "93.1"
        )
        private double ingestionSuccessPercent;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // INGESTION  —  detailed ingestion analytics
    // ─────────────────────────────────────────────────────────────────────────

    @Getter
    @Builder
    @Schema(
        name = "DashboardIngestionStats",
        description = "Detailed ingestion analytics including job counts by status, "
                    + "record totals, and per-type breakdowns"
    )
    public static class IngestionStats {

        // ── Job counts ────────────────────────────────────────────────────────

        @Schema(description = "Total ingestion jobs", example = "320")
        private long totalJobs;

        @Schema(description = "Jobs with status COMPLETED", example = "298")
        private long completedJobs;

        @Schema(description = "Jobs with status FAILED", example = "14")
        private long failedJobs;

        @Schema(description = "Jobs with status RUNNING", example = "1")
        private long runningJobs;

        @Schema(description = "Jobs with status PENDING", example = "2")
        private long pendingJobs;

        @Schema(description = "Jobs with status PARTIAL (some records failed)", example = "5")
        private long partialJobs;

        // ── Record counts ─────────────────────────────────────────────────────

        @Schema(description = "Total records across all jobs", example = "1300000")
        private long totalRecords;

        @Schema(description = "Records successfully processed", example = "1250000")
        private long processedRecords;

        @Schema(description = "Records that failed processing", example = "50000")
        private long failedRecords;

        @Schema(description = "Records pending synchronization", example = "3200")
        private long recordsPendingSync;

        @Schema(description = "Records already synchronized", example = "1246800")
        private long recordsSynchronized;

        // ── Rates ─────────────────────────────────────────────────────────────

        @Schema(description = "Job success rate as a percentage (0–100)", example = "93.1")
        private double jobSuccessPercent;

        @Schema(description = "Average records processed per completed job", example = "4194.0")
        private double avgRecordsPerJob;

        // ── Type breakdown ────────────────────────────────────────────────────

        @Schema(description = "Jobs ingested via CSV upload", example = "210")
        private long csvJobs;

        @Schema(description = "Jobs ingested via REST API", example = "95")
        private long apiJobs;

        @Schema(description = "Jobs triggered by the scheduler", example = "15")
        private long scheduledJobs;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SYNCHRONIZATION  —  detailed sync analytics
    // ─────────────────────────────────────────────────────────────────────────

    @Getter
    @Builder
    @Schema(
        name = "DashboardSynchronizationStats",
        description = "Detailed synchronization analytics including job counts, "
                    + "record totals, execution times, and success rate"
    )
    public static class SynchronizationStats {

        // ── Job counts ────────────────────────────────────────────────────────

        @Schema(description = "Total synchronization jobs", example = "180")
        private long totalJobs;

        @Schema(description = "Jobs with status COMPLETED", example = "172")
        private long completedJobs;

        @Schema(description = "Jobs with status FAILED", example = "8")
        private long failedJobs;

        @Schema(description = "Jobs currently RUNNING", example = "0")
        private long runningJobs;

        @Schema(description = "Jobs with status PENDING", example = "0")
        private long pendingJobs;

        // ── Record counts ─────────────────────────────────────────────────────

        @Schema(description = "Total records synchronized across all completed jobs", example = "1246800")
        private long totalRecordsSynchronized;

        @Schema(description = "Total records that failed sync validation", example = "3200")
        private long totalRecordsFailed;

        @Schema(description = "Records currently pending synchronization", example = "3200")
        private long recordsPendingSync;

        // ── Timing ────────────────────────────────────────────────────────────

        @Schema(description = "Timestamp of the last successful sync", example = "2026-05-23T10:15:45")
        private LocalDateTime lastSynchronizationTime;

        @Schema(description = "Average job execution time in milliseconds", example = "12450")
        private long avgExecutionTimeMs;

        @Schema(description = "Maximum job execution time in milliseconds", example = "45000")
        private long maxExecutionTimeMs;

        // ── Rate ──────────────────────────────────────────────────────────────

        @Schema(description = "Sync job success rate as a percentage (0–100)", example = "95.6")
        private double successPercent;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // AUDIT  —  audit log analytics
    // ─────────────────────────────────────────────────────────────────────────

    @Getter
    @Builder
    @Schema(
        name = "DashboardAuditStats",
        description = "Audit log analytics including total events, success/failure counts, "
                    + "distinct active users, and a breakdown of events by action type"
    )
    public static class AuditStats {

        @Schema(description = "Total audit log entries", example = "15420")
        private long totalEvents;

        @Schema(description = "Events with status SUCCESS", example = "14900")
        private long successfulEvents;

        @Schema(description = "Events with status FAILED", example = "520")
        private long failedEvents;

        @Schema(
            description = "Number of distinct usernames that appear in the audit log",
            example = "38"
        )
        private long distinctActiveUsers;

        @Schema(
            description = "Event success rate as a percentage (0–100)",
            example = "96.6"
        )
        private double eventSuccessPercent;

        @Schema(
            description = "Top audit actions with their event counts, "
                        + "sorted by count descending. Key = action name, Value = count.",
            example = "{\"USER_LOGIN\": 4200, \"INGEST_CSV\": 320, \"SYNC_COMPLETED\": 172}"
        )
        private Map<String, Long> eventsByAction;

        @Schema(
            description = "The 10 most recent audit log entries (action, username, status, timestamp)"
        )
        private List<RecentAuditEntry> recentEvents;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RECENT AUDIT ENTRY  —  lightweight projection used inside AuditStats
    // ─────────────────────────────────────────────────────────────────────────

    @Getter
    @Builder
    @Schema(
        name = "RecentAuditEntry",
        description = "Lightweight audit log entry used in the dashboard recent-events list"
    )
    public static class RecentAuditEntry {

        @Schema(description = "Audit action name", example = "USER_LOGIN")
        private String action;

        @Schema(description = "Username who performed the action", example = "alice")
        private String username;

        @Schema(description = "Outcome status", example = "SUCCESS")
        private String status;

        @Schema(description = "Brief details", example = "Login successful from 192.168.1.10")
        private String details;

        @Schema(description = "Event timestamp", example = "2026-05-23T10:14:02")
        private LocalDateTime timestamp;
    }
}
