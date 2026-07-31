package com.company.integrationplatform.circuitbreaker;

import com.company.integrationplatform.audit.AuditService;
import com.company.integrationplatform.common.Constants;
import com.company.integrationplatform.datasource.DataSourceEntity;
import com.company.integrationplatform.datasource.DataSourceNotFoundException;
import com.company.integrationplatform.datasource.DataSourceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Circuit Breaker service for data sources.
 *
 * <p>Operates ONE LEVEL ABOVE the per-job retry logic in {@code SyncExecutor}
 * and {@code IngestionService}. The retry loop (up to 3 attempts with exponential
 * backoff) is entirely invisible to this service — it only observes the final
 * outcome of each job execution: permanently COMPLETED or permanently FAILED.
 *
 * <h2>State machine</h2>
 * <pre>
 *  CLOSED ──[{@value #FAILURE_THRESHOLD} permanent failures]──► OPEN ──[suspended_until elapsed]──► HALF_OPEN
 *    ▲                                                                               │
 *    └──────────────────────[success]────────────────────────────────────────────────┘
 *                                              │
 *                               [failure]──► OPEN (doubled timeout, capped at {@value #MAX_SUSPENSION_MINUTES} min)
 * </pre>
 *
 * <h2>Thresholds (class-level constants)</h2>
 * <ul>
 *   <li>{@link #FAILURE_THRESHOLD} = 3 — permanent job failures to trip the circuit</li>
 *   <li>{@link #BASE_SUSPENSION_MINUTES} = 15 — initial cool-down on first open</li>
 *   <li>{@link #MAX_SUSPENSION_MINUTES} = 60 — cap on the doubling sequence</li>
 * </ul>
 *
 * <h2>Integration points</h2>
 * <ul>
 *   <li>{@link #recordSuccess(UUID)} — called by SyncExecutor and IngestionService on COMPLETED</li>
 *   <li>{@link #recordFailure(UUID)} — called by SyncExecutor and IngestionService on permanent FAILED</li>
 *   <li>{@link #isOpen(DataSourceEntity)} — called by SyncService scheduler to skip OPEN sources</li>
 *   <li>{@link #transitionToHalfOpenIfReady(DataSourceEntity)} — called by scheduler before the skip check</li>
 *   <li>{@link #manualReset(UUID, String)} — called by the admin reset endpoint</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CircuitBreakerService {

    // ── Thresholds ────────────────────────────────────────────────────────────

    /** Number of consecutive permanently-failed jobs before the circuit opens. */
    private static final int FAILURE_THRESHOLD = 3;

    /** Initial suspension window in minutes when the circuit first opens. */
    private static final long BASE_SUSPENSION_MINUTES = 15L;

    /** Maximum suspension window in minutes (doubles each re-trip, capped here). */
    private static final long MAX_SUSPENSION_MINUTES = 60L;

    // ── Dependencies ──────────────────────────────────────────────────────────

    private final DataSourceRepository dataSourceRepository;
    private final AuditService         auditService;

    // ─────────────────────────────────────────────────────────────────────────
    // PUBLIC API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Called after a job completes successfully (status COMPLETED or PARTIAL).
     *
     * <p>Resets the circuit to CLOSED and clears the failure counter.
     * If the circuit was HALF_OPEN, logs a recovery audit event.
     * If the circuit was already CLOSED with no failures, this is a no-op.
     *
     * @param dataSourceId the data source whose job just succeeded
     */
    @Transactional
    public void recordSuccess(UUID dataSourceId) {
        DataSourceEntity source = dataSourceRepository.findById(dataSourceId).orElse(null);
        if (source == null) {
            log.warn("CircuitBreaker.recordSuccess: data source {} not found, skipping.", dataSourceId);
            return;
        }

        CircuitState previousState = source.getCircuitState();
        boolean alreadyHealthy = previousState == CircuitState.CLOSED
                && source.getConsecutiveFailureCount() == 0;

        if (alreadyHealthy) {
            return; // Nothing to reset — fast path
        }

        // Reset to healthy state
        source.setCircuitState(CircuitState.CLOSED);
        source.setConsecutiveFailureCount(0);
        source.setSuspendedUntil(null);

        // Restore SourceStatus to ACTIVE if it was downgraded by the circuit breaker
        if (source.getStatus() == DataSourceEntity.SourceStatus.DEGRADED
                || source.getStatus() == DataSourceEntity.SourceStatus.SUSPENDED) {
            source.setStatus(DataSourceEntity.SourceStatus.ACTIVE);
        }
        dataSourceRepository.save(source);

        if (previousState == CircuitState.HALF_OPEN) {
            log.info("Circuit RECOVERED for '{}' (id={}): HALF_OPEN → CLOSED.",
                    source.getName(), dataSourceId);
            auditService.log(
                    Constants.ACTION_CIRCUIT_RECOVERED,
                    "SYSTEM",
                    "CLOSED",
                    String.format("Circuit breaker recovered: dataSource='%s' (id=%s). " +
                                  "HALF_OPEN test attempt succeeded. State reset to CLOSED.",
                            source.getName(), dataSourceId)
            );
        } else {
            log.info("Circuit failure counter cleared for '{}' (id={}): was {} failures, now CLOSED.",
                    source.getName(), dataSourceId, source.getConsecutiveFailureCount());
        }
    }

    /**
     * Called after a job permanently fails (all retries within that job exhausted).
     *
     * <p>Increments the consecutive failure counter. At {@link #FAILURE_THRESHOLD},
     * trips the circuit to OPEN and sets the suspension window.
     * If the circuit is already HALF_OPEN (a recovery test just failed),
     * re-trips immediately with a doubled suspension window.
     *
     * @param dataSourceId the data source whose job just permanently failed
     */
    @Transactional
    public void recordFailure(UUID dataSourceId) {
        DataSourceEntity source = dataSourceRepository.findById(dataSourceId).orElse(null);
        if (source == null) {
            log.warn("CircuitBreaker.recordFailure: data source {} not found, skipping.", dataSourceId);
            return;
        }

        CircuitState currentState = source.getCircuitState();
        source.setLastFailureAt(LocalDateTime.now());

        if (currentState == CircuitState.HALF_OPEN) {
            // ── Recovery test failed: re-trip with doubled suspension ──────────
            long nextSuspensionMinutes = computeNextSuspension(source);
            tripCircuit(source, nextSuspensionMinutes);

            log.warn("Circuit RE-TRIPPED for '{}' (id={}): HALF_OPEN test failed. " +
                     "Suspended for {} minutes until {}.",
                    source.getName(), dataSourceId,
                    nextSuspensionMinutes, source.getSuspendedUntil());
            auditService.log(
                    Constants.ACTION_CIRCUIT_OPENED,
                    "SYSTEM",
                    "OPEN",
                    String.format("Circuit breaker re-tripped: dataSource='%s' (id=%s). " +
                                  "HALF_OPEN recovery test failed. " +
                                  "Suspended for %d min until %s.",
                            source.getName(), dataSourceId,
                            nextSuspensionMinutes, source.getSuspendedUntil())
            );

        } else {
            // ── CLOSED: increment and check threshold ─────────────────────────
            int newCount = source.getConsecutiveFailureCount() + 1;
            source.setConsecutiveFailureCount(newCount);

            log.warn("Circuit breaker: permanent failure #{} for '{}' (id={}). Threshold={}/{}.",
                    newCount, source.getName(), dataSourceId, newCount, FAILURE_THRESHOLD);

            if (newCount >= FAILURE_THRESHOLD) {
                // ── Threshold reached: open the circuit ───────────────────────
                tripCircuit(source, BASE_SUSPENSION_MINUTES);

                log.error("Circuit OPENED for '{}' (id={}): {} consecutive failures " +
                          "(threshold={}). Suspended for {} min until {}.",
                        source.getName(), dataSourceId, newCount, FAILURE_THRESHOLD,
                        BASE_SUSPENSION_MINUTES, source.getSuspendedUntil());
                auditService.log(
                        Constants.ACTION_CIRCUIT_OPENED,
                        "SYSTEM",
                        "OPEN",
                        String.format("Circuit breaker OPENED: dataSource='%s' (id=%s). " +
                                      "%d consecutive permanent failures exceeded threshold of %d. " +
                                      "Suspended for %d min until %s. " +
                                      "Requires manual reset or auto-recovery after cooldown.",
                                source.getName(), dataSourceId,
                                newCount, FAILURE_THRESHOLD,
                                BASE_SUSPENSION_MINUTES, source.getSuspendedUntil())
                );
            } else {
                // ── Below threshold: just persist the updated counter ─────────
                dataSourceRepository.save(source);
            }
        }
    }

    /**
     * Checks whether this data source's circuit is currently OPEN and the
     * suspension window has NOT yet elapsed (i.e., the scheduler must skip it).
     *
     * <p>Returns {@code false} if the suspension window has elapsed, even if the
     * circuit is still technically OPEN — {@link #transitionToHalfOpenIfReady}
     * will handle the state transition on the same scheduler tick.
     *
     * @param source the data source entity (freshly loaded from DB)
     * @return {@code true} if the scheduler must skip this source entirely
     */
    public boolean isOpen(DataSourceEntity source) {
        if (source.getCircuitState() != CircuitState.OPEN) {
            return false;
        }
        // Suspension window still active → block
        if (source.getSuspendedUntil() != null
                && LocalDateTime.now().isBefore(source.getSuspendedUntil())) {
            return true;
        }
        // Suspension window elapsed → transition will happen, don't block
        return false;
    }

    /**
     * If a source is OPEN and its {@code suspended_until} has elapsed, transitions
     * it to HALF_OPEN so the scheduler allows exactly one test attempt this tick.
     *
     * <p><b>Call this BEFORE {@link #isOpen(DataSourceEntity)}</b> in the scheduler
     * loop so that a source ready for recovery gets its test attempt immediately
     * rather than waiting for the next scheduler cycle.
     *
     * @param source the data source entity (freshly loaded from DB)
     */
    @Transactional
    public void transitionToHalfOpenIfReady(DataSourceEntity source) {
        if (source.getCircuitState() != CircuitState.OPEN) return;
        if (source.getSuspendedUntil() == null) return;
        if (LocalDateTime.now().isBefore(source.getSuspendedUntil())) return;

        source.setCircuitState(CircuitState.HALF_OPEN);
        source.setStatus(DataSourceEntity.SourceStatus.DEGRADED);
        dataSourceRepository.save(source);

        log.info("Circuit OPEN → HALF_OPEN for '{}' (id={}). Suspension elapsed. " +
                 "One test attempt allowed.", source.getName(), source.getId());
        auditService.log(
                Constants.ACTION_CIRCUIT_HALF_OPEN,
                "SYSTEM",
                "HALF_OPEN",
                String.format("Circuit breaker HALF_OPEN: dataSource='%s' (id=%s). " +
                              "Suspension window elapsed. One test attempt will be made.",
                        source.getName(), source.getId())
        );
    }

    /**
     * Manually resets the circuit breaker to CLOSED for a given data source.
     *
     * <p>Intended for admin use after the underlying issue has been resolved.
     * Sets {@code circuit_state=CLOSED}, clears all failure counters and
     * suspension timestamps, and restores {@code status=ACTIVE}.
     *
     * @param dataSourceId  the data source to reset
     * @param adminUsername the username of the admin performing the reset (for audit)
     * @throws DataSourceNotFoundException if the data source does not exist
     */
    @Transactional
    public void manualReset(UUID dataSourceId, String adminUsername) {
        DataSourceEntity source = dataSourceRepository.findById(dataSourceId)
                .orElseThrow(() -> new DataSourceNotFoundException(dataSourceId));

        CircuitState previousState = source.getCircuitState();
        int previousFailures       = source.getConsecutiveFailureCount();

        source.setCircuitState(CircuitState.CLOSED);
        source.setConsecutiveFailureCount(0);
        source.setSuspendedUntil(null);
        source.setLastFailureAt(null);

        // Restore status to ACTIVE only if it was downgraded by the circuit breaker
        if (source.getStatus() == DataSourceEntity.SourceStatus.DEGRADED
                || source.getStatus() == DataSourceEntity.SourceStatus.SUSPENDED) {
            source.setStatus(DataSourceEntity.SourceStatus.ACTIVE);
        }
        dataSourceRepository.save(source);

        log.warn("Circuit breaker MANUALLY RESET by '{}' for '{}' (id={}). " +
                 "Previous state={}, failures={}.",
                adminUsername, source.getName(), dataSourceId, previousState, previousFailures);
        auditService.log(
                Constants.ACTION_CIRCUIT_RESET,
                adminUsername,
                "CLOSED",
                String.format("Circuit breaker manually reset by admin '%s': " +
                              "dataSource='%s' (id=%s). " +
                              "Previous state=%s, consecutive failures=%d. " +
                              "All counters cleared. Status restored to ACTIVE.",
                        adminUsername, source.getName(), dataSourceId,
                        previousState, previousFailures)
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATE HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Trips the circuit to OPEN: sets state, status, and suspension deadline,
     * then persists all three changes atomically.
     */
    private void tripCircuit(DataSourceEntity source, long suspensionMinutes) {
        source.setCircuitState(CircuitState.OPEN);
        source.setStatus(DataSourceEntity.SourceStatus.SUSPENDED);
        source.setSuspendedUntil(LocalDateTime.now().plusMinutes(suspensionMinutes));
        dataSourceRepository.save(source);
    }

    /**
     * Computes the next suspension window (in minutes) for a HALF_OPEN re-trip.
     *
     * <p>Attempts to infer the previous suspension duration from the DB timestamps,
     * then doubles it. Falls back to {@code BASE * 2} if timestamps are missing.
     * Always capped at {@link #MAX_SUSPENSION_MINUTES}.
     *
     * <p>Doubling sequence: 15 → 30 → 60 (capped).
     */
    private long computeNextSuspension(DataSourceEntity source) {
        if (source.getSuspendedUntil() != null && source.getLastFailureAt() != null) {
            long previousMinutes = Duration.between(
                    source.getLastFailureAt(), source.getSuspendedUntil()
            ).toMinutes();
            if (previousMinutes > 0) {
                return Math.min(previousMinutes * 2, MAX_SUSPENSION_MINUTES);
            }
        }
        // Fallback: jump straight to 2× base if we can't infer previous duration
        return Math.min(BASE_SUSPENSION_MINUTES * 2, MAX_SUSPENSION_MINUTES);
    }
}
