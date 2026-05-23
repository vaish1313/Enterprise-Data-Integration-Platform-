package com.company.integrationplatform.transformation;

import com.company.integrationplatform.audit.AuditService;
import com.company.integrationplatform.common.Constants;
import com.company.integrationplatform.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransformationService {

    private final TransformationRepository transformationRepository;
    private final AuditService auditService;

    /**
     * Apply all applicable transformation rules to a raw data record.
     * Rules are applied in executionOrder (ascending).
     */
    public Map<String, Object> transform(Map<String, Object> rawData, UUID dataSourceId) {
        List<TransformationRule> rules = transformationRepository.findApplicableRules(dataSourceId);
        Map<String, Object> result = new LinkedHashMap<>(rawData);

        for (TransformationRule rule : rules) {
            try {
                result = applyRule(result, rule);
            } catch (Exception e) {
                log.warn("Rule '{}' failed on data: {}", rule.getName(), e.getMessage());
            }
        }
        return result;
    }

    private Map<String, Object> applyRule(Map<String, Object> data, TransformationRule rule) {
        Map<String, Object> result = new LinkedHashMap<>(data);
        Map<String, String> config = rule.getConfig();
        if (config == null) return result;

        switch (rule.getRuleType()) {
            case FIELD_MAPPING -> {
                // Map source field names to target field names
                config.forEach((src, target) -> {
                    if (result.containsKey(src)) {
                        result.put(target, result.remove(src));
                    }
                });
            }
            case NORMALIZE -> {
                String field = config.get("field");
                String type  = config.getOrDefault("type", "TRIM");
                if (field != null && result.containsKey(field)) {
                    String val = String.valueOf(result.get(field));
                    result.put(field, switch (type.toUpperCase()) {
                        case "UPPERCASE" -> val.toUpperCase();
                        case "LOWERCASE" -> val.toLowerCase();
                        default          -> val.trim();
                    });
                }
            }
            case TYPE_CAST -> {
                String field      = config.get("field");
                String targetType = config.getOrDefault("targetType", "STRING");
                if (field != null && result.containsKey(field)) {
                    String val = String.valueOf(result.get(field));
                    result.put(field, switch (targetType.toUpperCase()) {
                        case "INTEGER" -> Integer.parseInt(val.trim());
                        case "DOUBLE"  -> Double.parseDouble(val.trim());
                        case "BOOLEAN" -> Boolean.parseBoolean(val.trim());
                        default        -> val;
                    });
                }
            }
            case RENAME -> {
                config.forEach((oldName, newName) -> {
                    if (result.containsKey(oldName)) {
                        result.put(newName, result.remove(oldName));
                    }
                });
            }
            case DEFAULT_VALUE -> {
                String field        = config.get("field");
                String defaultValue = config.get("defaultValue");
                if (field != null && defaultValue != null) {
                    result.putIfAbsent(field, defaultValue);
                }
            }
            case REMOVE_FIELD -> {
                config.keySet().forEach(result::remove);
            }
        }
        return result;
    }

    @Transactional
    public TransformationDto.Response createRule(TransformationDto.CreateRequest request) {
        String currentUser = currentUsername();
        TransformationRule rule = TransformationRule.builder()
                .name(request.getName())
                .dataSourceId(request.getDataSourceId())
                .ruleType(request.getRuleType())
                .config(request.getConfig())
                .executionOrder(request.getExecutionOrder())
                .enabled(true)
                .createdBy(currentUser)
                .build();
        TransformationRule saved = transformationRepository.save(rule);
        auditService.log(Constants.ACTION_TRANSFORM, currentUser, "SUCCESS",
                "Created transformation rule: " + saved.getName());
        return TransformationDto.Response.from(saved);
    }

    @Transactional(readOnly = true)
    public List<TransformationDto.Response> getAllRules() {
        return transformationRepository.findAll().stream()
                .map(TransformationDto.Response::from).toList();
    }

    @Transactional(readOnly = true)
    public TransformationDto.Response getRuleById(UUID id) {
        return TransformationDto.Response.from(findById(id));
    }

    @Transactional
    public TransformationDto.Response updateRule(UUID id, TransformationDto.UpdateRequest request) {
        TransformationRule rule = findById(id);
        if (request.getName()           != null) rule.setName(request.getName());
        if (request.getConfig()         != null) rule.setConfig(request.getConfig());
        if (request.getEnabled()        != null) rule.setEnabled(request.getEnabled());
        if (request.getExecutionOrder() != null) rule.setExecutionOrder(request.getExecutionOrder());
        return TransformationDto.Response.from(transformationRepository.save(rule));
    }

    @Transactional
    public void deleteRule(UUID id) {
        transformationRepository.delete(findById(id));
    }

    private TransformationRule findById(UUID id) {
        return transformationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TransformationRule", id));
    }

    private String currentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
