package com.company.integrationplatform.synchronization;

import com.company.integrationplatform.ingestion.IngestionRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Validates ingestion records before they are synchronized to the target system.
 *
 * <p><b>Validation rules applied:</b>
 * <ol>
 *   <li>Record must have a non-null {@code transformedData} map</li>
 *   <li>Transformed data must not be empty</li>
 *   <li>Record status must be {@code PROCESSED} (not FAILED or SKIPPED)</li>
 *   <li>Record must not already be synchronized</li>
 * </ol>
 *
 * <p>This component is intentionally kept simple and stateless.
 * Additional domain-specific validation rules can be added here
 * without touching the sync engine.
 */
@Slf4j
@Component
public class SyncRecordValidator {

    /**
     * Validates a single ingestion record for synchronization readiness.
     *
     * @param record the record to validate
     * @return a {@link ValidationResult} indicating pass/fail and reason
     */
    public ValidationResult validate(IngestionRecord record) {

        // Rule 1: Must have transformed data
        if (record.getTransformedData() == null) {
            return ValidationResult.fail("Record has no transformed data — "
                    + "run transformation before sync");
        }

        // Rule 2: Transformed data must not be empty
        if (record.getTransformedData().isEmpty()) {
            return ValidationResult.fail("Transformed data map is empty — "
                    + "transformation may have removed all fields");
        }

        // Rule 3: Status must be PROCESSED
        if (record.getStatus() != IngestionRecord.RecordStatus.PROCESSED) {
            return ValidationResult.fail("Record status is " + record.getStatus()
                    + " — only PROCESSED records can be synchronized");
        }

        // Rule 4: Must not already be synchronized
        if (record.isSynchronized_()) {
            return ValidationResult.skip("Record already synchronized by job "
                    + record.getSyncJobId());
        }

        return ValidationResult.pass();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RESULT TYPE
    // ─────────────────────────────────────────────────────────────────────────

    public enum Outcome { PASS, FAIL, SKIP }

    public record ValidationResult(Outcome outcome, String reason) {

        public static ValidationResult pass() {
            return new ValidationResult(Outcome.PASS, null);
        }

        public static ValidationResult fail(String reason) {
            return new ValidationResult(Outcome.FAIL, reason);
        }

        public static ValidationResult skip(String reason) {
            return new ValidationResult(Outcome.SKIP, reason);
        }

        public boolean isPassed() { return outcome == Outcome.PASS; }
        public boolean isFailed() { return outcome == Outcome.FAIL; }
        public boolean isSkipped() { return outcome == Outcome.SKIP; }
    }
}
