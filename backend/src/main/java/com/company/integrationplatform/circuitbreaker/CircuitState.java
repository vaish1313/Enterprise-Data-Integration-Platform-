package com.company.integrationplatform.circuitbreaker;

/**
 * Represents the state of the circuit breaker for a data source.
 *
 * <ul>
 *   <li>{@code CLOSED}    — Normal operation. Jobs run freely.</li>
 *   <li>{@code OPEN}      — Circuit tripped. Scheduler skips this source entirely.
 *                           Auto-transitions to HALF_OPEN after {@code suspended_until}
 *                           has elapsed.</li>
 *   <li>{@code HALF_OPEN} — One cautious test attempt is allowed.
 *                           Success → CLOSED. Failure → OPEN (doubled timeout).</li>
 * </ul>
 *
 * <pre>
 *  CLOSED ──[3 permanent failures]──► OPEN ──[suspended_until elapsed]──► HALF_OPEN
 *    ▲                                                                          │
 *    └─────────────────[success]──────────────────────────────────────────────┘
 *                                          │
 *                           [failure]──► OPEN (doubled timeout, cap 60 min)
 * </pre>
 */
public enum CircuitState {
    CLOSED,
    OPEN,
    HALF_OPEN
}
