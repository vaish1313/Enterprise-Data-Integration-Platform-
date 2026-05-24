package com.company.integrationplatform.transformation;

import com.company.integrationplatform.audit.AuditService;
import com.company.integrationplatform.common.Constants;
import com.company.integrationplatform.common.PageResponse;
import com.company.integrationplatform.exception.ResourceNotFoundException;
import com.company.integrationplatform.exception.ValidationException;
import com.company.integrationplatform.ingestion.IngestionJob;
import com.company.integrationplatform.ingestion.IngestionRecord;
import com.company.integrationplatform.ingestion.IngestionRecordRepository;
import com.company.integrationplatform.ingestion.IngestionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service layer for the Transformation Rules Engine.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>CRUD for transformation rules with validation and audit logging</li>
 *   <li>Inline transformation during ingestion (called by IngestionService)</li>
 *   <li>Post-ingestion apply: re-apply rules to all records of an existing job</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransformationService {

    private final TransformationRepository   transformationRepository;
    private final IngestionRepository        ingestionRepository;
    private final IngestionRecordRepository  recordRepository;
    private final TransformationEngine       engine;
    private final AuditService               auditService;

    // ─────────────────────────────────────────────────────────────────────────
    // INLINE TRANSFORM  (called by IngestionService during CSV parsing)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Applies all active rules for the given data source to a single raw record.
     * Rules are applied in executionOrder. Failures on individual rules are logged
     * and skipped — they do not abort the entire record.
     *
     * @param rawData      the raw key-value map from the CSV row
     * @param dataSourceId the data source being ingested
     * @return the transformed map
     */
    public Map<String, Object> transform(Map<String, Object> rawData, UUID dataSourceId) {
        List<TransformationRule> rules =
                transformationRepository.findActiveRulesForDataSource(dataSourceId);

        Map<String, Object> result = rawData;
        for (TransformationRule rule : rules) {
            try {
                result = engine.apply(result, rule);
            } catch (Exception e) {
                log.warn("Rule '{}' skipped on inline transform: {}", rule.getName(), e.getMessage());
            }
        }
        return result;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // APPLY TO JOB  (POST /transformation-rules/apply/{jobId})
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Re-applies all active transformation rules to every record of an existing
     * ingestion job. Updates the {@code transformedData} field on each record.
     *
     * <p>This is useful when rules are added or changed after ingestion has already run.
     *
     * @param jobId the ingestion job to re-transform
     * @return a detailed {@link TransformationDto.ApplyResult}
     * @throws ResourceNotFoundException if the job does not exist
     */
    @Transactional
    public TransformationDto.ApplyResult applyToJob(UUID jobId) {
        String currentUser = currentUsername();
        LocalDateTime startedAt = LocalDateTime.now();

        // Validate job exists
        IngestionJob job = ingestionRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("IngestionJob", jobId));

        // Load active rules for this job's data source
        List<TransformationRule> rules =
                transformationRepository.findActiveRulesForDataSource(job.getDataSourceId());

        if (rules.isEmpty()) {
            log.info("No active rules found for dataSource={}, jobId={}",
                    job.getDataSourceId(), jobId);
        }

        // Load all records for this job
        List<IngestionRecord> records = recordRepository.findByJobId(jobId);

        // Per-rule counters
        List<int[]> ruleCounters = new ArrayList<>(); // [applied, skipped, error] per rule
        for (int i = 0; i < rules.size(); i++) {
            ruleCounters.add(new int[]{0, 0, 0});
        }

        int transformed = 0, failed = 0;
        List<IngestionRecord> toSave = new ArrayList<>();

        for (IngestionRecord record : records) {
            if (record.getRawData() == null) {
                failed++;
                continue;
            }

            Map<String, Object> result = record.getRawData();
            boolean recordFailed = false;

            for (int i = 0; i < rules.size(); i++) {
                TransformationRule rule = rules.get(i);
                int[] counters = ruleCounters.get(i);
                try {
                    Map<String, Object> after = engine.apply(result, rule);
                    // Check if the rule actually changed anything
                    if (!after.equals(result)) {
                        counters[0]++; // applied
                    } else {
                        counters[1]++; // skipped (no change)
                    }
                    result = after;
                } catch (TransformationExecutionException e) {
                    log.warn("Rule '{}' failed on record row={}: {}",
                            rule.getName(), record.getSourceRowNumber(), e.getMessage());
                    counters[2]++; // error
                    recordFailed = true;
                }
            }

            record.setTransformedData(result);
            record.setStatus(recordFailed
                    ? IngestionRecord.RecordStatus.FAILED
                    : IngestionRecord.RecordStatus.PROCESSED);
            toSave.add(record);

            if (recordFailed) failed++; else transformed++;

            // Batch save every 500 records
            if (toSave.size() >= 500) {
                recordRepository.saveAll(toSave);
                toSave.clear();
            }
        }

        if (!toSave.isEmpty()) {
            recordRepository.saveAll(toSave);
        }

        // Build per-rule summaries
        List<TransformationDto.RuleSummary> summaries = new ArrayList<>();
        for (int i = 0; i < rules.size(); i++) {
            TransformationRule rule = rules.get(i);
            int[] c = ruleCounters.get(i);
            summaries.add(TransformationDto.RuleSummary.builder()
                    .ruleId(rule.getId())
                    .ruleName(rule.getName())
                    .transformationType(rule.getTransformationType())
                    .appliedCount(c[0])
                    .skippedCount(c[1])
                    .errorCount(c[2])
                    .build());
        }

        LocalDateTime completedAt = LocalDateTime.now();

        auditService.log(
                Constants.ACTION_TRANSFORMATION_EXECUTED,
                currentUser,
                failed == 0 ? "SUCCESS" : "PARTIAL",
                String.format("Applied %d rules to jobId=%s: total=%d, transformed=%d, failed=%d",
                        rules.size(), jobId, records.size(), transformed, failed)
        );

        log.info("Transformation applied: jobId={}, rules={}, total={}, transformed={}, failed={}",
                jobId, rules.size(), records.size(), transformed, failed);

        return TransformationDto.ApplyResult.builder()
                .jobId(jobId)
                .totalRecords(records.size())
                .transformedRecords(transformed)
                .failedRecords(failed)
                .rulesApplied(rules.size())
                .ruleSummaries(summaries)
                .startedAt(startedAt)
                .completedAt(completedAt)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CRUD — CREATE
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public TransformationDto.Response createRule(TransformationDto.CreateRequest request) {
        String currentUser = currentUsername();
        validateUniqueName(request.getName(), request.getDataSourceId(), null);
        validateRuleConfig(request.getTransformationType(), request.getSourceField(),
                request.getTargetField(), request.getDefaultValue(), request.getExtraConfig());

        TransformationRule rule = TransformationRule.builder()
                .name(request.getName().trim())
                .description(request.getDescription())
                .dataSourceId(request.getDataSourceId())
                .transformationType(request.getTransformationType())
                .sourceField(request.getSourceField())
                .targetField(request.getTargetField())
                .defaultValue(request.getDefaultValue())
                .extraConfig(request.getExtraConfig())
                .executionOrder(request.getExecutionOrder())
                .active(true)
                .createdBy(currentUser)
                .build();

        TransformationRule saved = transformationRepository.save(rule);

        auditService.log(
                Constants.ACTION_CREATE_RULE,
                currentUser,
                "SUCCESS",
                String.format("Created rule: name='%s', type=%s, id=%s",
                        saved.getName(), saved.getTransformationType(), saved.getId())
        );

        log.info("Transformation rule created: id={}, name={}, type={}, user={}",
                saved.getId(), saved.getName(), saved.getTransformationType(), currentUser);

        return TransformationDto.Response.from(saved);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CRUD — READ
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PageResponse<TransformationDto.Response> getAllRules(Pageable pageable) {
        Page<TransformationDto.Response> page = transformationRepository
                .findAll(pageable)
                .map(TransformationDto.Response::from);
        return PageResponse.of(page);
    }

    @Transactional(readOnly = true)
    public TransformationDto.Response getRuleById(UUID id) {
        return TransformationDto.Response.from(findById(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<TransformationDto.Response> getRulesByDataSource(UUID dataSourceId,
                                                                          Pageable pageable) {
        Page<TransformationDto.Response> page = transformationRepository
                .findByDataSourceId(dataSourceId, pageable)
                .map(TransformationDto.Response::from);
        return PageResponse.of(page);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CRUD — UPDATE
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public TransformationDto.Response updateRule(UUID id, TransformationDto.UpdateRequest request) {
        TransformationRule rule = findById(id);
        String currentUser = currentUsername();

        if (request.getName() != null && !request.getName().isBlank()) {
            validateUniqueName(request.getName().trim(), rule.getDataSourceId(), id);
            rule.setName(request.getName().trim());
        }
        if (request.getDescription()       != null) rule.setDescription(request.getDescription());
        if (request.getTransformationType() != null) rule.setTransformationType(request.getTransformationType());
        if (request.getSourceField()        != null) rule.setSourceField(request.getSourceField());
        if (request.getTargetField()        != null) rule.setTargetField(request.getTargetField());
        if (request.getDefaultValue()       != null) rule.setDefaultValue(request.getDefaultValue());
        if (request.getExtraConfig()        != null) rule.setExtraConfig(request.getExtraConfig());
        if (request.getActive()             != null) rule.setActive(request.getActive());
        if (request.getExecutionOrder()     != null) rule.setExecutionOrder(request.getExecutionOrder());

        TransformationRule saved = transformationRepository.save(rule);

        auditService.log(
                Constants.ACTION_UPDATE_RULE,
                currentUser,
                "SUCCESS",
                String.format("Updated rule: name='%s', type=%s, active=%s, id=%s",
                        saved.getName(), saved.getTransformationType(), saved.isActive(), saved.getId())
        );

        return TransformationDto.Response.from(saved);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CRUD — DELETE
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public void deleteRule(UUID id) {
        TransformationRule rule = findById(id);
        String currentUser = currentUsername();

        transformationRepository.delete(rule);

        auditService.log(
                Constants.ACTION_DELETE_RULE,
                currentUser,
                "SUCCESS",
                String.format("Deleted rule: name='%s', type=%s, id=%s",
                        rule.getName(), rule.getTransformationType(), rule.getId())
        );

        log.info("Transformation rule deleted: id={}, name={}, user={}",
                rule.getId(), rule.getName(), currentUser);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // VALIDATION
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Validates that the rule name is unique within its data source scope.
     */
    private void validateUniqueName(String name, UUID dataSourceId, UUID excludeId) {
        boolean conflict = (excludeId == null)
                ? transformationRepository.existsByNameAndDataSourceId(name, dataSourceId)
                : transformationRepository.existsByNameAndDataSourceIdAndIdNot(name, dataSourceId, excludeId);

        if (conflict) {
            throw new ValidationException(
                    "A transformation rule named '" + name + "' already exists for this data source scope");
        }
    }

    /**
     * Validates that required config fields are present for the given transformation type.
     */
    private void validateRuleConfig(TransformationRule.TransformationType type,
                                     String sourceField, String targetField,
                                     String defaultValue, String extraConfig) {
        switch (type) {
            case DIRECT_MAPPING, UPPERCASE, LOWERCASE, TRIM -> {
                if (sourceField == null || sourceField.isBlank()) {
                    throw new ValidationException(
                            type + " requires sourceField to be specified");
                }
            }
            case CONCAT -> {
                if (sourceField == null || sourceField.isBlank()) {
                    throw new ValidationException("CONCAT requires sourceField");
                }
                if (extraConfig == null || extraConfig.isBlank()) {
                    throw new ValidationException(
                            "CONCAT requires extraConfig (the second field name to concatenate)");
                }
                if (targetField == null || targetField.isBlank()) {
                    throw new ValidationException("CONCAT requires targetField");
                }
            }
            case DEFAULT_VALUE -> {
                if (sourceField == null || sourceField.isBlank()) {
                    throw new ValidationException("DEFAULT_VALUE requires sourceField");
                }
                if (defaultValue == null) {
                    throw new ValidationException(
                            "DEFAULT_VALUE requires defaultValue (the fallback value)");
                }
            }
            case DATE_FORMAT -> {
                if (sourceField == null || sourceField.isBlank()) {
                    throw new ValidationException("DATE_FORMAT requires sourceField");
                }
                if (extraConfig == null || extraConfig.isBlank()) {
                    throw new ValidationException(
                            "DATE_FORMAT requires extraConfig (input date pattern, e.g. 'dd/MM/yyyy')");
                }
                if (defaultValue == null || defaultValue.isBlank()) {
                    throw new ValidationException(
                            "DATE_FORMAT requires defaultValue (output date pattern, e.g. 'yyyy-MM-dd')");
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    private TransformationRule findById(UUID id) {
        return transformationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TransformationRule", id));
    }

    private String currentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
