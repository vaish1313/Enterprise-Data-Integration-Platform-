package com.company.integrationplatform.audit;

import com.company.integrationplatform.common.PageResponse;
import com.company.integrationplatform.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditRepository auditRepository;

    // ─────────────────────────────────────────────────────────────────────────
    // INTERNAL LOGGING  (called by other services — not exposed via HTTP)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Asynchronously persists an audit log entry in its own transaction.
     * Using REQUIRES_NEW ensures the audit write is never rolled back
     * if the caller's transaction fails.
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(String action, String username, String status, String details) {
        try {
            AuditEntity entry = AuditEntity.builder()
                    .action(action)
                    .username(username != null ? username : "SYSTEM")
                    .status(status)
                    .details(details)
                    .timestamp(LocalDateTime.now())
                    .build();
            auditRepository.save(entry);
        } catch (Exception e) {
            log.error("Failed to persist audit log: action={}, user={}, error={}",
                    action, username, e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // READ  —  ADMIN | ANALYST | OPERATOR
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PageResponse<AuditDto> getAll(Pageable pageable) {
        return PageResponse.of(auditRepository.findAll(pageable).map(AuditDto::from));
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditDto> getByUser(String username, Pageable pageable) {
        return PageResponse.of(
                auditRepository.findByUsername(username, pageable).map(AuditDto::from));
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditDto> getByAction(String action, Pageable pageable) {
        return PageResponse.of(
                auditRepository.findByAction(action, pageable).map(AuditDto::from));
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditDto> getByDateRange(LocalDateTime from, LocalDateTime to,
                                                  Pageable pageable) {
        return PageResponse.of(
                auditRepository.findByTimestampBetween(from, to, pageable).map(AuditDto::from));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // EXPORT  —  ADMIN | ANALYST  (OPERATOR denied at controller layer)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Streams all audit log entries to the provided writer as CSV.
     * Uses a direct DB query (no pagination) to support large exports
     * without loading everything into heap at once.
     */
    @Transactional(readOnly = true)
    public void exportToCsv(PrintWriter writer) throws IOException {
        writeCsvHeader(writer);
        List<AuditEntity> entries = auditRepository.findAllByOrderByTimestampDesc();
        for (AuditEntity e : entries) {
            writeCsvRow(writer, e);
        }
        writer.flush();
    }

    /**
     * Streams audit log entries within a date range to the provided writer as CSV.
     */
    @Transactional(readOnly = true)
    public void exportRangeToCsv(LocalDateTime from, LocalDateTime to,
                                  PrintWriter writer) throws IOException {
        writeCsvHeader(writer);
        List<AuditEntity> entries =
                auditRepository.findByTimestampBetweenOrderByTimestampDesc(from, to);
        for (AuditEntity e : entries) {
            writeCsvRow(writer, e);
        }
        writer.flush();
    }

    private void writeCsvHeader(PrintWriter writer) {
        writer.println("id,action,username,status,details,ip_address,timestamp");
    }

    private void writeCsvRow(PrintWriter writer, AuditEntity e) {
        writer.printf("%s,%s,%s,%s,\"%s\",%s,%s%n",
                e.getId(),
                escapeCsv(e.getAction()),
                escapeCsv(e.getUsername()),
                escapeCsv(e.getStatus()),
                escapeCsv(e.getDetails()),
                escapeCsv(e.getIpAddress()),
                e.getTimestamp());
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        return value.replace("\"", "\"\"");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DELETE  —  ADMIN only  (enforced at controller layer)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Permanently deletes a single audit log entry by ID.
     *
     * @throws ResourceNotFoundException if no entry exists with the given ID
     */
    @Transactional
    public void deleteById(UUID id) {
        AuditEntity entry = auditRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AuditLog", id));
        auditRepository.delete(entry);
        log.warn("Audit log entry deleted: id={}", id);
    }

    /**
     * Bulk-deletes all audit log entries older than {@code days} days.
     *
     * @param days retention window in days (entries older than this are deleted)
     * @return number of entries deleted
     */
    @Transactional
    public long purgeOlderThan(int days) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(days);
        int deleted = auditRepository.deleteByTimestampBefore(cutoff);
        log.warn("Audit log purge: deleted {} entries older than {} days (cutoff={})",
                deleted, days, cutoff);
        return deleted;
    }
}
